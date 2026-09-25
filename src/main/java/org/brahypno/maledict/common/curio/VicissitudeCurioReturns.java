package org.brahypno.maledict.common.curio;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
 * <p>Delivery order: original slot, any legal empty Curio slot, then main inventory.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VicissitudeCurioReturns {
    public static final String REMAINING_MESSAGE_KEY = "message.maledict.first_vicissitude.curio_return_pending";
    private static final String COMPLETED_MESSAGE_KEY = "message.maledict.first_vicissitude.curio_return_complete";
    /** Low-frequency retry for owners that are online but were short on space. */
    private static final int QUEUE_INTERVAL_TICKS = 20;
    private static final int ATTEMPT_COOLDOWN_TICKS = 100;

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

    /** Tries to deliver everything held for this player; true when nothing remains owed. */
    public static boolean deliverAll(ServerPlayer player) {
        VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(player.serverLevel());
        List<VicissitudeCurioLedger.Entry> entries = ledger.entries(player.getUUID());
        if (entries.isEmpty()) {
            return true;
        }
        List<VicissitudeCurioLedger.Entry> remaining = new ArrayList<>();
        for (VicissitudeCurioLedger.Entry entry : entries) {
            ItemStack remainder = deliver(player, entry);
            if (!remainder.isEmpty()) {
                remaining.add(new VicissitudeCurioLedger.Entry(entry.slotIdentifier(),
                        entry.slotIndex(), remainder));
            }
        }
        ledger.replace(player.getUUID(), remaining);
        ledger.markAttempt(player.getUUID(), player.level().getGameTime());
        if (remaining.isEmpty()) {
            consumeTokens(player);
            player.displayClientMessage(Component.translatable(COMPLETED_MESSAGE_KEY), true);
            return true;
        }
        issueToken(player, remaining.size());
        return false;
    }

    /**
     * Returns the undelivered remainder. Only the delivered part leaves the record, so a partial
     * inventory add can never duplicate or delete a stack.
     */
    private static ItemStack deliver(ServerPlayer player, VicissitudeCurioLedger.Entry entry) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        ItemStack stack = entry.stack().copy();
        if (inventory != null) {
            if (equip(inventory, entry.slotIdentifier(), entry.slotIndex(), stack)) {
                return ItemStack.EMPTY;
            }
            List<String> identifiers = new ArrayList<>(inventory.getCurios().keySet());
            Collections.sort(identifiers);
            for (String identifier : identifiers) {
                ICurioStacksHandler handler = inventory.getCurios().get(identifier);
                if (handler == null) {
                    continue;
                }
                for (int slot = 0; slot < handler.getStacks().getSlots(); slot++) {
                    if (identifier.equals(entry.slotIdentifier()) && slot == entry.slotIndex()) {
                        continue;
                    }
                    if (equip(inventory, identifier, slot, stack)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        if (player.getInventory().add(stack)) {
            return ItemStack.EMPTY;
        }
        // No space anywhere: never drop during death processing and never delete the record.
        return stack;
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
        deliverAll(player);
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % QUEUE_INTERVAL_TICKS != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            VicissitudeCurioLedger ledger = VicissitudeCurioLedger.get(level);
            for (UUID owner : ledger.owners()) {
                ServerPlayer player = server.getPlayerList().getPlayer(owner);
                if (player == null || !ledger.hasPending(owner)) {
                    continue;
                }
                if (level.getGameTime() - ledger.lastAttempt(owner) < ATTEMPT_COOLDOWN_TICKS) {
                    continue;
                }
                deliverAll(player);
            }
        }
    }
}
