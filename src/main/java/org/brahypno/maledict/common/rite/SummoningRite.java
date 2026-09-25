package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.config.MaledictConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

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

    /** Creates the configured entity above the totem, in the same tick the rite resolves. */
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
        float yaw = totem.getDirection().toYRot();
        if (!place(created, level, x, base.getY(), z, yaw)) {
            // Every candidate was blocked; a rite that promises uncontrolled creation must not
            // quietly do nothing, and the encounter brings its own unstick search.
            created.moveTo(x, base.getY() + SUMMON_HEIGHTS[0], z, yaw, 0.0F);
        }
        level.addFreshEntity(created);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, created.getX(),
                created.getY() + created.getBbHeight() * 0.5D, created.getZ(), 64, 1.2D, 1.6D, 1.2D,
                0.02D);
        level.playSound(null, x, base.getY(), z, SoundRegistry.SOUL_SHATTER.get(),
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
        return ForgeRegistries.ENTITY_TYPES.getValue(identity);
    }

    private SummoningRite() {
    }
}
