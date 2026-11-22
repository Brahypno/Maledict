package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.Set;

@EventBusSubscriber(modid = Maledict.MODID)
public final class BlightedRealmEvents {
    // These plains animals all use Animal.checkAnimalSpawnRules; other species keep their own rules.
    private static final Set<EntityType<?>> FARM_ANIMALS = Set.of(EntityType.COW, EntityType.SHEEP,
            EntityType.PIG, EntityType.CHICKEN, EntityType.HORSE, EntityType.DONKEY);

    @SubscribeEvent
    public static void weather(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide() && BlightedRealm.is(event.getLevel())
                && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            // Other dimensions can share the overworld's weather data; clear only this level's interpolation.
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }

    @SubscribeEvent
    public static void animalGround(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!BlightedRealm.is(event.getLevel().getLevel()) || event.getResult() != MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT
                || event.getDefaultResult() || !FARM_ANIMALS.contains(event.getEntityType())) return;
        var ground = event.getLevel().getBlockState(event.getPos().below());
        if (ground.is(MalumBlocks.BLIGHTED_EARTH.get())
                && event.getLevel().getRawBrightness(event.getPos(), 0) > 8) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        }
    }

    @SubscribeEvent
    public static void noSleeping(PlayerInteractEvent.RightClickBlock event) {
        if (!BlightedRealm.is(event.getLevel())) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BedBlock) && !state.is(Blocks.RESPAWN_ANCHOR)) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!event.getLevel().isClientSide()) {
            event.getEntity().displayClientMessage(Component.translatable("message.maledict.realm_no_respawn"), true);
        }
    }

    @SubscribeEvent
    public static void noRespawn(PlayerSetSpawnEvent event) {
        if (event.getSpawnLevel().equals(BlightedRealm.LEVEL)) event.setCanceled(true);
    }

    /**
     * The realm hands its visitors its own book: Malum's Encyclopedia Esoterica becomes the
     * Lightfall Codex, whose Arcana view keeps the same entries and adds the Void and Lightfall
     * pages. The swap is in place, so the slot and count stay where they were.
     *
     * <p>1.20.1 是「新堆叠 + {@code setTag(stack.getTag().copy())}」——1.21 没有根 NBT，
     * 换 {@link ItemStack#transmuteCopy} 更准：它换物品与数量、把原件上的组件补丁整份搬过去，
     * 自定义名、附魔光效之类都不会丢，正是 1.20.1 那句 {@code copy()} 的意图。
     */
    @SubscribeEvent
    public static void replaceCodexOnEntry(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(BlightedRealm.LEVEL) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean replaced = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(MalumItems.ENCYCLOPEDIA_ESOTERICA.get())) continue;
            inventory.setItem(slot, stack.transmuteCopy(MaledictItems.LIGHTFALL_CODEX.get(), stack.getCount()));
            replaced = true;
        }
        if (replaced) inventory.setChanged();
    }

    @SubscribeEvent
    public static void emergenceFall(LivingFallEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var data = player.getPersistentData();
            if (data.contains(WeepingWellTravel.EMERGENCE_UNTIL)) {
                if (data.getLong(WeepingWellTravel.EMERGENCE_UNTIL) >= player.server.overworld().getGameTime()) {
                    event.setCanceled(true);
                }
                data.remove(WeepingWellTravel.EMERGENCE_UNTIL);
            }
        }
    }

    private BlightedRealmEvents() {
    }
}
