package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.config.MaledictConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;


/**
 * Registers the encounter's four rites in Malum's rite table and performs what they create.
 */
public final class SummoningRite {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Never stack creations: one is already a boss encounter. */
    private static final double SUMMON_GUARD_RADIUS = 64.0D;
    /** Candidates for the spawn height above the totem base, tried in order. */
    private static final int[] SUMMON_HEIGHTS = {3, 4, 6, 2, 8};

    /**
     * One recipe per difficulty, cheapest first; poles are read bottom first, so a recipe is written
     * as "eldritch spirits at the bottom, then arcane spirits above". Only the eight pole spirits may
     * be used: {@code MalumLogBLock#createTotemPole} returns false outright for {@code UMBRAL_SPIRIT}.
     */
    private enum Recipe {
        SIMPLE("vicissitude_rite", BossDifficulty.SIMPLE, 3, 0),
        DIFFICULT("greater_vicissitude_rite", BossDifficulty.DIFFICULT, 4, 0),
        COMPLETE("eldritch_vicissitude_rite", BossDifficulty.COMPLETE, 3, 1),
        EXTREME("greater_eldritch_vicissitude_rite", BossDifficulty.EXTREME, 3, 2);

        private final String identifier;
        private final BossDifficulty difficulty;
        private final int arcane;
        private final int eldritch;

        Recipe(String identifier, BossDifficulty difficulty, int arcane, int eldritch) {
            this.identifier = identifier;
            this.difficulty = difficulty;
            this.arcane = arcane;
            this.eldritch = eldritch;
        }
    }

    public static VicissitudeRiteType rite(BossDifficulty difficulty) {
        return org.brahypno.maledict.registry.MaledictRites.summoning(difficulty);
    }

    /** Creates the configured entity; Vicissitude enters through a suspended rift. */
    public static void summon(TotemBaseBlockEntity totem, ServerLevel level, BossDifficulty difficulty) {
        if (!MaledictConfig.SUMMONING_RITE.get()) {
            return;
        }
        EntityType<?> type = resolveType();
        if (type == null) {
            return;
        }
        BlockPos base = totem.getBlockPos();
        double x = base.getX() + 0.5D;
        double z = base.getZ() + 0.5D;
        AABB guard = new AABB(base).inflate(SUMMON_GUARD_RADIUS);
        if (!level.getEntitiesOfClass(Entity.class, guard, entity -> entity.getType() == type)
                .isEmpty()) {
            return;
        }
        Entity created = type.create(level);
        if (created == null) {
            return;
        }
        if (created instanceof FirstVicissitudeBossEntity boss) {
            boss.setBossDifficulty(difficulty);
        }
        float yaw = totem.getTotemDirection().toYRot();
        if (!place(created, level, x, base.getY(), z, yaw)) {
            // Every candidate was blocked; a rite that promises uncontrolled creation must not
            // quietly do nothing, and the encounter brings its own unstick search.
            created.moveTo(x, base.getY() + SUMMON_HEIGHTS[0], z, yaw, 0.0F);
        }
        if (created instanceof FirstVicissitudeBossEntity boss) {
            boss.beginRiteSummoning(totem);
            level.addFreshEntity(boss);
            return;
        }
        level.addFreshEntity(created);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, created.getX(),
                created.getY() + created.getBbHeight() * 0.5D, created.getZ(), 64, 1.2D, 1.6D, 1.2D,
                0.02D);
        level.playSound(null, x, base.getY(), z, MalumSoundEvents.SOUL_SHATTER.get(),
                SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    /** Finds a free spot above the totem; a big hitbox needs the clearance checked, not assumed. */
    private static boolean place(Entity entity, ServerLevel level, double x, double baseY,
                                 double z, float yaw) {
        for (int height : SUMMON_HEIGHTS) {
            entity.moveTo(x, baseY + height, z, yaw, 0.0F);
            if (level.noCollision(entity)) {
                return true;
            }
        }
        return false;
    }

    private static EntityType<?> resolveType() {
        String name = MaledictConfig.SUMMONING_RITE_ENTITY.get();
        ResourceLocation identity = ResourceLocation.tryParse(name);
        if (identity == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(identity);
    }

    private SummoningRite() {
    }
}
