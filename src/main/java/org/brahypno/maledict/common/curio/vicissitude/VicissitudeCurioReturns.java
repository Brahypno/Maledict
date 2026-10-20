package org.brahypno.maledict.common.curio.vicissitude;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Confiscation and return of Curios for the Vicissitude encounter; confiscation skips stacks in
 * {@code maledict:vicissitude_confiscation_immune}.
 *
 * <p>Original identifier and index only; rebuild dependent slots before falling back to the
 * backpack. Once the encounter ends, remaining overflow goes into chests near the boss.
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeCurioReturns {
    public static final String REMAINING_MESSAGE_KEY = "message.maledict.first_vicissitude.curio_return_pending";
    private static final String COMPLETED_MESSAGE_KEY = "message.maledict.first_vicissitude.curio_return_complete";
    /** Low-frequency retry for owners that are online but were short on space. */
    private static final int QUEUE_INTERVAL_TICKS = 20;

    private VicissitudeCurioReturns() {
    }

    public static List<VicissitudeCurioLedger.Entry> confiscate(ServerPlayer player) {
        List<VicissitudeCurioLedger.Entry> taken = new ArrayList<>();
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return taken;
        }
        List<String> identifiers = new ArrayList<>(inventory.getCurios().keySet());
        Collections.sort(identifiers);
        for (String identifier : identifiers) {
            ICurioStacksHandler handler = inventory.getCurios().get(identifier);
            if (handler == null) {
                continue;
            }
            IDynamicStackHandler stacks = handler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                ItemStack equipped = stacks.getStackInSlot(slot);
                if (equipped.isEmpty() || equipped.is(MaledictTags.VICISSITUDE_CONFISCATION_IMMUNE)) {
                    // Protected curios stay equipped and never fire unequip hooks.
                    continue;
                }
                taken.add(new VicissitudeCurioLedger.Entry(identifier, slot, equipped.copy()));
                inventory.setEquippedCurio(identifier, slot, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    /** Hands boss-held stacks to the world ledger without dropping or deleting anything. */
    public static void retain(@Nullable ServerLevel level, UUID owner,
                              List<VicissitudeCurioLedger.Entry> entries) {
        if (level == null || entries.isEmpty()) {
            return;
        }
        VicissitudeCurioLedger.get(level).addAll(owner, entries);
    }

    public static void retain(ServerLevel level, UUID owner,
                              List<VicissitudeCurioLedger.Entry> entries, UUID encounter) {
        retain(level, owner, entries.stream().map(entry -> new VicissitudeCurioLedger.Entry(
                entry.slotIdentifier(), entry.slotIndex(), entry.stack(), encounter, null)).toList());
    }

    public static void finishEncounter(ServerLevel level, UUID encounter, BlockPos position) {
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(level);
        ledger.finishEncounter(encounter, new VicissitudeCurioLedger.ReturnSite(level.dimension().location(), position));
        for (UUID owner : ledger.owners()) {
            if (ledger.entries(owner).stream().noneMatch(entry -> encounter.equals(entry.encounter()))) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) deliverAll(player);
        }
    }

    /** Tries to deliver everything held for this player; true when nothing remains owed. */
    public static boolean deliverAll(ServerPlayer player) {
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(player.serverLevel());
        UUID owner = player.getUUID();
        if (!ledger.hasPending(owner)) return true;
        // CurioEquipEvent listeners can claim again while validation is still in progress.
        if (!ledger.beginDelivery(owner)) return false;
        try {
            List<VicissitudeCurioLedger.Entry> entries = ledger.entries(owner);
            List<VicissitudeCurioLedger.Entry> remaining = new ArrayList<>();
            ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
            boolean equipped = false;
            for (var entry : entries) {
                ItemStack stack = entry.stack().copy();
                if (inventory != null && equip(inventory, entry.slotIdentifier(), entry.slotIndex(), stack)) {
                    equipped = true;
                } else {
                    remaining.add(entry);
                }
            }
            long now = player.level().getGameTime();
            // Native Curios onEquip/slot modifiers run on the next living tick, not setEquippedCurio.
            // Also give a newly loaded player's equipment one tick before declaring slots absent.
            if (equipped || ledger.lastAttempt(owner) == Long.MIN_VALUE) ledger.equipmentChanged(owner, now);
            if (!remaining.isEmpty() && ledger.equipmentHasSettled(owner)) {
                // The claim token must not occupy the last inventory space owed to a real item.
                consumeTokens(player);
                List<VicissitudeCurioLedger.Entry> overflow = new ArrayList<>();
                for (var entry : remaining) {
                    ItemStack stack = entry.stack().copy();
                    player.getInventory().add(stack);
                    if (!stack.isEmpty()) overflow.add(entry.withStack(stack));
                }
                remaining = placeOverflowChests(player, overflow);
            }
            // A validation callback may release another item; do not overwrite that new debt.
            for (var added : ledger.entries(owner)) if (!entries.contains(added)) remaining.add(added);
            ledger.replace(owner, remaining);
            if (remaining.isEmpty()) {
                consumeTokens(player);
                player.displayClientMessage(Component.translatable(COMPLETED_MESSAGE_KEY), true);
                return true;
            }
            ledger.markAttempt(owner, now);
            issueToken(player, remaining.size());
            return false;
        } finally {
            ledger.endDelivery(owner);
        }
    }

    private static List<VicissitudeCurioLedger.Entry> placeOverflowChests(ServerPlayer player,
            List<VicissitudeCurioLedger.Entry> entries) {
        List<VicissitudeCurioLedger.Entry> remaining = new ArrayList<>();
        ChestBlockEntity chest = null;
        VicissitudeCurioLedger.ReturnSite chestSite = null;
        int slot = 0;
        for (var entry : entries) {
            ItemStack stack = entry.stack().copy();
            while (!stack.isEmpty() && entry.returnSite() != null) {
                if (chest == null || slot == chest.getContainerSize() || !entry.returnSite().equals(chestSite)) {
                    chest = placeChest(player, entry.returnSite());
                    chestSite = entry.returnSite();
                    slot = 0;
                }
                if (chest == null) break;
                chest.setItem(slot++, stack.split(Math.min(stack.getMaxStackSize(), chest.getMaxStackSize())));
            }
            if (!stack.isEmpty()) remaining.add(entry.withStack(stack));
        }
        if (chest != null) chest.setChanged();
        return remaining;
    }

    @Nullable
    private static ChestBlockEntity placeChest(ServerPlayer player, VicissitudeCurioLedger.ReturnSite site) {
        ServerLevel level = player.serverLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, site.dimension()));
        if (level == null) return null;
        // Search a small area around the saved boss position; never load chunks or replace blocks.
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : new int[]{0, 1, -1}) {
                        BlockPos pos = site.position().offset(dx, dy, dz);
                        if (!level.isLoaded(pos)
                                || !level.getWorldBorder().isWithinBounds(pos) || !level.isEmptyBlock(pos)) continue;
                        if (level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3)
                                && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                            chest.applyComponents(chest.collectComponents(), net.minecraft.core.component.DataComponentPatch.builder()
                                    .set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, player.getDisplayName().copy().append(" ")
                                            .append(Component.translatable("container.chest"))).build());
                            return chest;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean equip(ICuriosItemHandler inventory, String identifier, int slot,
                                 ItemStack stack) {
        ICurioStacksHandler handler = inventory.getCurios().get(identifier);
        if (handler == null || slot < 0 || slot >= handler.getStacks().getSlots()) {
            return false;
        }
        IDynamicStackHandler stacks = handler.getStacks();
        if (!stacks.getStackInSlot(slot).isEmpty() || !stacks.isItemValid(slot, stack)) {
            return false;
        }
        // Validation fires a mod event: its listeners may fill or resize this slot.
        if (inventory.getCurios().get(identifier) != handler || slot >= stacks.getSlots()
                || !stacks.getStackInSlot(slot).isEmpty()) return false;
        inventory.setEquippedCurio(identifier, slot, stack.copy());
        stack.setCount(0);
        return true;
    }

    /** Guarantees the player holds exactly one claim token while a balance is undeliverable. */
    private static void issueToken(ServerPlayer player, int remaining) {
        boolean hasToken = player.getInventory().items.stream()
                .anyMatch(stack -> stack.is(MaledictItems.CURIO_RETURN_TOKEN.get()))
                           || player.getInventory().offhand.stream()
                                   .anyMatch(stack -> stack.is(MaledictItems.CURIO_RETURN_TOKEN.get()));
        if (!hasToken) {
            ItemStack token = new ItemStack(MaledictItems.CURIO_RETURN_TOKEN.get());
            if (!player.getInventory().add(token)) {
                // Even the token does not fit; the ledger keeps the balance and retries later.
                return;
            }
        }
        player.displayClientMessage(Component.translatable(REMAINING_MESSAGE_KEY, remaining), true);
    }

    private static void consumeTokens(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(MaledictItems.CURIO_RETURN_TOKEN.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Used by the token item: claim whatever fits right now. */
    public static boolean claim(ServerPlayer player) {
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(player.serverLevel());
        if (!ledger.hasPending(player.getUUID())) {
            player.displayClientMessage(Component.translatable(COMPLETED_MESSAGE_KEY), true);
            return false;
        }
        deliverAll(player);
        return true;
    }

    public static void onPlayerDeath(ServerPlayer player) {
        // Death only makes the player eligible for an immediate return; the boss keeps its own hold.
        deliverAll(player);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(player.serverLevel());
        if (ledger.hasPending(player.getUUID())) ledger.equipmentChanged(player.getUUID(), player.level().getGameTime());
        deliverAll(player);
    }

    /** Curios applies equipment and slot attributes at NORMAL priority on this same event. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEquipmentTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (livingEntity instanceof ServerPlayer player) {
            VicissitudeCurioLedger.get(player.serverLevel()).equipmentUpdated(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % QUEUE_INTERVAL_TICKS != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(server.overworld());
        for (UUID owner : ledger.owners()) {
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player == null || !ledger.hasPending(owner)) {
                continue;
            }
            long last = ledger.lastAttempt(owner);
            if (last != Long.MIN_VALUE && player.level().getGameTime() - last < QUEUE_INTERVAL_TICKS) {
                continue;
            }
            deliverAll(player);
        }
    }
}
