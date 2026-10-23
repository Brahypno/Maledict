package org.brahypno.maledict.common.vicissitude.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.SpiritRiteRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.vicissitude.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictEntities;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Registers the encounter's four rites in Malum's rite table and performs what they create.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Rite types are plain static state, so this only has to run after Malum's own class init.
        event.enqueueWork(SummoningRite::install);
    }

    public static void install() {
        for (Recipe recipe : Recipe.values()) {
            if (SpiritRiteRegistry.getRite(recipe.identifier) != null) {
                continue;
            }
            MalumSpiritType[] spirits = spiritsOf(recipe);
            if (!allPoleSpirits(spirits)) {
                LOGGER.warn("Skipping rite {}: its recipe uses a spirit Malum's totem poles refuse",
                        recipe.identifier);
                continue;
            }
            SpiritRiteRegistry.create(new VicissitudeRiteType(recipe.identifier, recipe.difficulty,
                    spirits));
        }
    }

    /** Bottom pole first: the eldritch tier sits under the arcane spirits. */
    private static MalumSpiritType[] spiritsOf(Recipe recipe) {
        List<MalumSpiritType> spirits = new ArrayList<>(recipe.arcane + recipe.eldritch);
        for (int index = 0; index < recipe.eldritch; index++) {
            spirits.add(SpiritTypeRegistry.ELDRITCH_SPIRIT);
        }
        for (int index = 0; index < recipe.arcane; index++) {
            spirits.add(SpiritTypeRegistry.ARCANE_SPIRIT);
        }
        return spirits.toArray(new MalumSpiritType[0]);
    }

    private static boolean allPoleSpirits(MalumSpiritType[] spirits) {
        for (MalumSpiritType spirit : spirits) {
            if (!poleSpirit(spirit)) {
                return false;
            }
        }
        return true;
    }

    /** The one spirit a totem pole will not take; every other registered shard is fine. */
    private static boolean poleSpirit(MalumSpiritType spirit) {
        return spirit != null && !SpiritTypeRegistry.UMBRAL_SPIRIT.equals(spirit);
    }

    /** The rite that summons the given difficulty, or null before common setup has run. */
    @Nullable
    public static VicissitudeRiteType rite(BossDifficulty difficulty) {
        for (Recipe recipe : Recipe.values()) {
            if (recipe.difficulty == difficulty) {
                return SpiritRiteRegistry.getRite(recipe.identifier)
                               instanceof VicissitudeRiteType rite ? rite : null;
            }
        }
        return null;
    }

    /** Creates the configured entity; Vicissitude enters through a suspended rift. */
    static void summon(TotemBaseBlockEntity totem, ServerLevel level, BossDifficulty difficulty) {
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
        if (hasNearbySummon(level, base, type)) {
            return;
        }
        BlockPos landing = findLanding(level, base, type);
        if (landing == null) {
            return;
        }
        Entity created = type.create(level);
        if (created == null) {
            return;
        }
        if (created instanceof FirstVicissitudeBossEntity boss) {
            boss.setBossDifficulty(difficulty);
        }
        float yaw = totem.getDirection().toYRot();
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
        level.playSound(null, x, base.getY(), z, SoundRegistry.SOUL_SHATTER.get(),
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
            if (!level.hasChunksAt(BlockPos.containing(space.minX, space.minY, space.minZ),
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
        return ForgeRegistries.ENTITY_TYPES.getValue(identity);
    }

    private SummoningRite() {
    }
}
