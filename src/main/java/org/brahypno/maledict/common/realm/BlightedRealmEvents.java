package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;

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
