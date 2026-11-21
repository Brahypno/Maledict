package org.brahypno.maledict.common.entity.vicissitude.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.brahypno.maledict.registry.MaledictEntities;
import org.jetbrains.annotations.Nullable;
import java.util.Set;
import java.util.HashSet;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import org.brahypno.maledict.common.MaledictChunks;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.config.MaledictConfig;


/**
 * Creates the configured encounters for the registered Vicissitude rites.
 */
public final class SummoningRite {
    /** Never stack creations: one is already a boss encounter. */
    private static final double SUMMON_GUARD_RADIUS = 64.0D;
    /** Candidates for the spawn height above the totem base, tried in order. */
    private static final int[] SUMMON_HEIGHTS = {3, 4, 6, 2, 8};

    public static VicissitudeRiteType rite(BossDifficulty difficulty) {
        return org.brahypno.maledict.registry.MaledictRites.summoning(difficulty);
    }

    /** Creates the configured entity; Vicissitude enters through a suspended rift. */
    public static void summon(TotemBaseBlockEntity totem, ServerLevel level, BossDifficulty difficulty) {
        // Peaceful never spawns the encounter, so the rite must not consume the totem there either; the
        // click handler refuses the same way. VicissitudeSpawnEvents would drop the boss after assembly.
        if (!MaledictConfig.SUMMONING_RITE.get() || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        EntityType<?> type = resolveType();
        if (type == null) {
            return;
        }
        BlockPos base = totem.getBlockPos();
        double x = base.getX() + 0.5D;
        double z = base.getZ() + 0.5D;
        if (hasNearbySummon(level, base, type)) return;
        BlockPos landing = findLanding(level, base, type);
        if (landing == null) return;
        Entity created = type.create(level);
        if (created == null) {
            return;
        }
        if (created instanceof FirstVicissitudeBossEntity boss) {
            boss.setBossDifficulty(difficulty);
        }
        float yaw = totem.getTotemDirection().toYRot();
        created.moveTo(x, landing.getY(), z, yaw, 0.0F);
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

    /** Same exclusion check before confirmation and immediately before entity creation. */
    static boolean hasNearbySummon(ServerLevel level, BlockPos base) {
        EntityType<?> type = resolveType();
        return type != null && hasNearbySummon(level, base, type);
    }

    private static boolean hasNearbySummon(ServerLevel level, BlockPos base, EntityType<?> type) {
        AABB guard = new AABB(base).inflate(SUMMON_GUARD_RADIUS);
        return !level.getEntitiesOfClass(Entity.class, guard, entity -> entity.getType() == type).isEmpty();
    }

    static boolean hasSummoningSpace(ServerLevel level, BlockPos base) {
        EntityType<?> type = resolveType();
        // Invalid configuration is handled by summon, rather than reported as cramped terrain.
        return type == null || findLanding(level, base, type) != null;
    }

    /** Search uses exactly the same clearance before confirmation and before entity creation. */
    @Nullable
    static BlockPos findLanding(ServerLevel level, BlockPos base, EntityType<?> type) {
        Set<BlockPos> consumed = consumedBlocks(level, base);
        for (int height : SUMMON_HEIGHTS) {
            BlockPos landing = base.above(height);
            AABB space = clearance(landing, type);
            if (space.minY < level.getMinBuildHeight() || space.maxY > level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(space)) continue;
            if (!MaledictChunks.loaded(level, BlockPos.containing(space.minX, space.minY, space.minZ),
                    BlockPos.containing(space.maxX, space.maxY, space.maxZ))) continue;
            boolean clear = true;
            var volume = Shapes.create(space);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(space.minX, space.minY, space.minZ),
                    BlockPos.containing(Math.nextDown(space.maxX), Math.nextDown(space.maxY), Math.nextDown(space.maxZ)))) {
                if (consumed.contains(pos)) continue;
                var state = level.getBlockState(pos);
                // Fluids and visible non-solid blocks can also obscure the entrance.
                if (!state.getFluidState().isEmpty() || Shapes.joinIsNotEmpty(volume,
                        state.getShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) {
                    clear = false;
                    break;
                }
            }
            if (clear) return landing;
        }
        return null;
    }

    static AABB clearance(BlockPos landing, EntityType<?> type) {
        double radius = type.getWidth() * 0.5D;
        double height = type.getHeight();
        double below = 0;
        if (type == MaledictEntities.FIRST_VICISSITUDE.get()) {
            radius = Math.max(radius, VicissitudeSummoning.CLEARANCE_RADIUS);
            height = VicissitudeSummoning.RISE + Math.max(height, VicissitudeSummoning.CLEARANCE_HEIGHT);
            below = VicissitudeSummoning.CLEARANCE_BELOW;
        }
        double x = landing.getX() + 0.5D, z = landing.getZ() + 0.5D;
        return new AABB(x - radius, landing.getY() - below, z - radius,
                x + radius, landing.getY() + height, z + radius);
    }

    /** Only blocks the existing consumption sequence actually removes are exempt. */
    private static Set<BlockPos> consumedBlocks(ServerLevel level, BlockPos base) {
        Set<BlockPos> consumed = new HashSet<>();
        consumed.add(base);
        BlockPos top = base;
        while (top.getY() + 1 < level.getMaxBuildHeight()
                && level.getBlockEntity(top.above()) instanceof TotemPoleBlockEntity) {
            top = top.above();
            consumed.add(top);
        }
        final BlockPos poleTop = top;
        for (int y : VicissitudeTotemConsumption.findUpperLogs(top.getY(), level.getMaxBuildHeight(),
                height -> level.getBlockState(poleTop.atY(height)).isAir(),
                height -> VicissitudeTotemConsumption.isRitualLog(level.getBlockState(poleTop.atY(height))))) {
            consumed.add(top.atY(y));
        }
        return consumed;
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
