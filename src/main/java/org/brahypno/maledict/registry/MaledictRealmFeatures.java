package org.brahypno.maledict.registry;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedDetailsFeature;
import org.brahypno.maledict.common.realm.BlightedRemnantFeature;
import org.brahypno.maledict.common.realm.FallenSoulwoodFeature;
import org.brahypno.maledict.common.realm.AbandonedWellStationFeature;

public final class MaledictRealmFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Maledict.MODID);
    public static final RegistryObject<BlightedDetailsFeature> DETAILS = FEATURES.register("blighted_details", BlightedDetailsFeature::new);
    public static final RegistryObject<FallenSoulwoodFeature> FALLEN_TREE = FEATURES.register("fallen_soulwood", FallenSoulwoodFeature::new);
    public static final RegistryObject<BlightedRemnantFeature> FAILED_RITE = FEATURES.register("failed_rite", () -> new BlightedRemnantFeature(false));
    public static final RegistryObject<BlightedRemnantFeature> ABANDONED_DIG = FEATURES.register("abandoned_dig", () -> new BlightedRemnantFeature(true));
    public static final RegistryObject<AbandonedWellStationFeature> WELL_STATION = FEATURES.register("abandoned_well_station", AbandonedWellStationFeature::new);

    private MaledictRealmFeatures() {}
}
