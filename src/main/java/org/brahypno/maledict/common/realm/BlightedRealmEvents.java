package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

import java.util.Set;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlightedRealmEvents {
    // These plains animals all use Animal.checkAnimalSpawnRules; other species keep their own rules.
    private static final Set<EntityType<?>> FARM_ANIMALS = Set.of(EntityType.COW, EntityType.SHEEP,
            EntityType.PIG, EntityType.CHICKEN, EntityType.HORSE, EntityType.DONKEY);

    @SubscribeEvent
    public static void weather(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && BlightedRealm.is(event.level)
                && event.level instanceof net.minecraft.server.level.ServerLevel level) {
            // Other dimensions can share the overworld's weather data; clear only this level's interpolation.
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }

    @SubscribeEvent
    public static void animalGround(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!BlightedRealm.is(event.getLevel().getLevel()) || event.getResult() != Event.Result.DEFAULT
                || event.getDefaultResult() || !FARM_ANIMALS.contains(event.getEntityType())) return;
        var ground = event.getLevel().getBlockState(event.getPos().below());
        if ((ground.is(BlockRegistry.BLIGHTED_EARTH.get()) || ground.is(BlockRegistry.BLIGHTED_SOIL.get()))
                && event.getLevel().getRawBrightness(event.getPos(), 0) > 8) {
            event.setResult(Event.Result.ALLOW);
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
