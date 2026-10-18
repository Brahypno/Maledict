package org.brahypno.maledict.common.realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.brahypno.maledict.Maledict;

public final class BlightedRealm {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_realm");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    public static final ResourceKey<Biome> WASTES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_wastes"));
    public static final double COORDINATE_SCALE = 16.0D;

    public static boolean is(Level level) {
        return level.dimension().equals(LEVEL);
    }

    private BlightedRealm() {
    }
}
