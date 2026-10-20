package org.brahypno.maledict.data.worldgen;

import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.worldgen.MalumFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MaledictWorldgen extends DatapackBuiltinEntriesProvider {
    private static final ResourceLocation TREES_ID = id("blighted_soulwood");
    private static final ResourceLocation GROWTH_ID = id("blighted_growth");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES = ResourceKey.create(Registries.CONFIGURED_FEATURE, TREES_ID);
    public static final ResourceKey<ConfiguredFeature<?, ?>> GROWTH = ResourceKey.create(Registries.CONFIGURED_FEATURE, GROWTH_ID);
    public static final ResourceKey<PlacedFeature> PLACED_TREES = ResourceKey.create(Registries.PLACED_FEATURE, TREES_ID);
    public static final ResourceKey<PlacedFeature> PLACED_GROWTH = ResourceKey.create(Registries.PLACED_FEATURE, GROWTH_ID);
    public static final ResourceKey<ConfiguredFeature<?, ?>> LEAD_ORE = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("ore_malignant_lead"));
    public static final ResourceKey<PlacedFeature> PLACED_LEAD_ORE = ResourceKey.create(Registries.PLACED_FEATURE, id("ore_malignant_lead"));
    public static final ResourceKey<PlacedFeature> PLACED_LEAD_ORE_LOWER = ResourceKey.create(Registries.PLACED_FEATURE, id("ore_malignant_lead_lower"));

    public MaledictWorldgen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, MaledictWorldgen::features)
                .add(Registries.PLACED_FEATURE, MaledictWorldgen::placements)
                .add(Registries.BIOME, MaledictWorldgen::biomes)
                .add(Registries.NOISE_SETTINGS, MaledictWorldgen::noise)
                .add(Registries.DIMENSION_TYPE, MaledictWorldgen::dimensionType)
                .add(Registries.LEVEL_STEM, MaledictWorldgen::dimension), Set.of(Maledict.MODID));
    }

    private static void features(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(TREES, new ConfiguredFeature<>(MalumFeatures.SOULWOOD_TREE.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(GROWTH, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(MalumBlocks.BLIGHTED_GROWTH.get()))));
        // Match vanilla ORE_GOLD_BURIED; registry holders cannot be dereferenced during this bootstrap.
        context.register(LEAD_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new BlockMatchTest(MalumBlocks.TAINTED_ROCK.get()),
                        MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get().defaultBlockState()),
                OreConfiguration.target(new BlockMatchTest(MalumBlocks.TWISTED_ROCK.get()),
                        MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get().defaultBlockState())),
                9, 0.5F)));
    }

    private static void placements(BootstrapContext<PlacedFeature> context) {
        var features = context.lookup(Registries.CONFIGURED_FEATURE);
        // Broad noise patches provide woods and open ground inside the initial single biome.
        context.register(PLACED_TREES, new PlacedFeature(features.getOrThrow(TREES), List.of(
                NoiseThresholdCountPlacement.of(-0.2D, 0, 2), RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BiomeFilter.biome())));
        context.register(PLACED_GROWTH, new PlacedFeature(features.getOrThrow(GROWTH), List.of(
                CountPlacement.of(24), InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG), BiomeFilter.biome())));
        // Match vanilla ORE_GOLD / ORE_GOLD_LOWER, changing only the configured feature.
        context.register(PLACED_LEAD_ORE, new PlacedFeature(features.getOrThrow(LEAD_ORE),
                List.of(CountPlacement.of(4), InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)),
                        BiomeFilter.biome())));
        context.register(PLACED_LEAD_ORE_LOWER, new PlacedFeature(features.getOrThrow(LEAD_ORE),
                List.of(CountPlacement.of(UniformInt.of(0, 1)), InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)),
                        BiomeFilter.biome())));
    }

    private static void biomes(BootstrapContext<Biome> context) {
        var generation = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE),
                context.lookup(Registries.CONFIGURED_CARVER));
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        // Keep useful underground soil/gravel and lichen without inserting ordinary decorative stone deposits.
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIRT);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRAVEL);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.GLOW_LICHEN);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        // Retain the complete default ore list, substituting only its two gold placements in this biome.
        var defaultOres = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE),
                context.lookup(Registries.CONFIGURED_CARVER));
        BiomeDefaultFeatures.addDefaultOres(defaultOres);
        var oreSteps = defaultOres.build().features();
        var placements = context.lookup(Registries.PLACED_FEATURE);
        for (int step = 0; step < oreSteps.size(); step++) {
            for (var feature : oreSteps.get(step)) {
                generation.addFeature(step, feature.is(OrePlacements.ORE_GOLD) ? placements.getOrThrow(PLACED_LEAD_ORE)
                        : feature.is(OrePlacements.ORE_GOLD_LOWER) ? placements.getOrThrow(PLACED_LEAD_ORE_LOWER) : feature);
            }
        }
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, PLACED_TREES);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, PLACED_GROWTH);
        var mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.plainsSpawns(mobs);
        mobs.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 3, 1, 4));
        mobs.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.COD, 10, 3, 6));
        mobs.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1));
        context.register(BlightedRealm.WASTES, new Biome.BiomeBuilder()
                .hasPrecipitation(false).temperature(0.7F).downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x16121E).fogColor(0x54415E)
                        .waterColor(0x30243E).waterFogColor(0x241B30)
                        .grassColorOverride(0x584467).foliageColorOverride(0x42334F).build())
                .mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build());
    }

    private static void noise(BootstrapContext<NoiseGeneratorSettings> context) {
        var vanilla = NoiseGeneratorSettings.overworld(context, false, false);
        var earth = SurfaceRules.state(MalumBlocks.BLIGHTED_EARTH.get().defaultBlockState());
        var soil = SurfaceRules.state(MalumBlocks.BLIGHTED_EARTH.get().defaultBlockState());
        var rock = SurfaceRules.state(MalumBlocks.TAINTED_ROCK.get().defaultBlockState());
        var deepRock = SurfaceRules.state(MalumBlocks.TWISTED_ROCK.get().defaultBlockState());
        var surface = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(),
                        VerticalAnchor.aboveBottom(5)), SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.steep(), rock), earth)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, soil))),
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0),
                        VerticalAnchor.absolute(8)), deepRock));
        context.register(BlightedRealm.NOISE, new NoiseGeneratorSettings(vanilla.noiseSettings(),
                MalumBlocks.TAINTED_ROCK.get().defaultBlockState(), vanilla.defaultFluid(), vanilla.noiseRouter(), surface,
                vanilla.spawnTarget(), vanilla.seaLevel(), false, true, true, false));
    }

    private static void dimensionType(BootstrapContext<DimensionType> context) {
        context.register(BlightedRealm.TYPE, new DimensionType(OptionalLong.of(18000L), true, false,
                false, false, BlightedRealm.COORDINATE_SCALE, false, false, -64, 384, 384,
                BlockTags.INFINIBURN_OVERWORLD, BlightedRealm.ID, 0.12F,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)));
    }

    private static void dimension(BootstrapContext<LevelStem> context) {
        context.register(BlightedRealm.STEM, new LevelStem(context.lookup(Registries.DIMENSION_TYPE).getOrThrow(BlightedRealm.TYPE),
                new NoiseBasedChunkGenerator(new FixedBiomeSource(context.lookup(Registries.BIOME).getOrThrow(BlightedRealm.WASTES)),
                        context.lookup(Registries.NOISE_SETTINGS).getOrThrow(BlightedRealm.NOISE))));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, path);
    }
}
