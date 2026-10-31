package org.brahypno.maledict.registry;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedDetailsFeature;
import org.brahypno.maledict.common.realm.BlightedRemnantFeature;
import org.brahypno.maledict.common.realm.FallenSoulwoodFeature;
import org.brahypno.maledict.common.realm.AbandonedWellStationFeature;

public final class MaledictRealmFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, Maledict.MODID);
    public static final DeferredHolder<Feature<?>, BlightedDetailsFeature> DETAILS = FEATURES.register("blighted_details", BlightedDetailsFeature::new);
    public static final DeferredHolder<Feature<?>, FallenSoulwoodFeature> FALLEN_TREE = FEATURES.register("fallen_soulwood", FallenSoulwoodFeature::new);
    public static final DeferredHolder<Feature<?>, BlightedRemnantFeature> FAILED_RITE = FEATURES.register("failed_rite", () -> new BlightedRemnantFeature(false));
    public static final DeferredHolder<Feature<?>, BlightedRemnantFeature> ABANDONED_DIG = FEATURES.register("abandoned_dig", () -> new BlightedRemnantFeature(true));
    public static final DeferredHolder<Feature<?>, AbandonedWellStationFeature> WELL_STATION = FEATURES.register("abandoned_well_station", AbandonedWellStationFeature::new);

    private MaledictRealmFeatures() {}
}
