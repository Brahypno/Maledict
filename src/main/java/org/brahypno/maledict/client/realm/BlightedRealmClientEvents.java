package org.brahypno.maledict.client.realm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlightedRealmClientEvents {
    private static ClientLevel ambientLevel;
    private static SimpleSoundInstance ambientSound;
    private static int ambientDelay = 100;

    @SubscribeEvent
    public static void weather(TickEvent.ClientTickEvent event) {
        var level = Minecraft.getInstance().level;
        if (event.phase == TickEvent.Phase.END && level != null && BlightedRealm.is(level)) {
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }

    @SubscribeEvent
    public static void ambience(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level != ambientLevel) {
            if (ambientSound != null) minecraft.getSoundManager().stop(ambientSound);
            ambientSound = null;
            ambientLevel = level;
            ambientDelay = 100;
        }
        if (level == null || !BlightedRealm.is(level) || minecraft.player == null || minecraft.isPaused()) return;
        var random = level.random;
        BlockPos origin = minecraft.player.blockPosition();
        if (level.getGameTime() % 8 == 0) {
            BlockPos leaf = origin.offset(random.nextInt(17) - 8, random.nextInt(9), random.nextInt(17) - 8);
            if (level.hasChunkAt(leaf) && level.getBlockState(leaf).is(BlockTags.LEAVES) && level.isEmptyBlock(leaf.below())) {
                level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, level.getBlockState(leaf)),
                        leaf.getX() + random.nextDouble(), leaf.getY() - 0.05, leaf.getZ() + random.nextDouble(), 0, 0, 0);
            }
        }
        if (--ambientDelay > 0) return;
        ambientDelay = 180 + random.nextInt(240);
        if (minecraft.player.isUnderWater() || ambientSound != null && minecraft.getSoundManager().isActive(ambientSound)) return;
        // Only actual nearby water, wood and leaves emit positional sounds.
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos source = origin.offset(random.nextInt(17) - 8, random.nextInt(7) - 3, random.nextInt(17) - 8);
            if (!level.hasChunkAt(source)) continue;
            var state = level.getBlockState(source);
            var sound = state.getFluidState().is(FluidTags.WATER) ? SoundEvents.WATER_AMBIENT
                    : state.is(BlockTags.LOGS) ? SoundEvents.BAMBOO_WOOD_HIT
                    : state.is(BlockTags.LEAVES) ? SoundEvents.AZALEA_LEAVES_STEP : null;
            if (sound == null) continue;
            ambientSound = new SimpleSoundInstance(sound, SoundSource.AMBIENT, 0.25F, 0.65F + random.nextFloat() * 0.2F,
                    random, source);
            minecraft.getSoundManager().play(ambientSound);
            return;
        }
        if (level.canSeeSky(origin)) {
            // Quiet wind, reusing an existing vanilla recording; silence remains between passages.
            ambientSound = SimpleSoundInstance.forLocalAmbience(SoundEvents.ELYTRA_FLYING, 0.65F, 0.035F);
            minecraft.getSoundManager().play(ambientSound);
        }
    }

    private BlightedRealmClientEvents() {
    }
}
