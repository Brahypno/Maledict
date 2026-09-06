package org.brahypno.maledict.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MaledictConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue AERIAL_POTION_EFFECTS_ONLY;
    public static final ForgeConfigSpec.IntValue EARTHEN_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue AQUEOUS_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue ARCANE_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue AERIAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue SACRED_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue INFERNAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue ELDRITCH_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.IntValue WICKED_UPGRADE_COST_COEFFICIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("incursusBlade");
        AERIAL_POTION_EFFECTS_ONLY = builder
                .comment("When true, Aerial Spirit can only grant beneficial effects used by registered potions.")
                .define("aerialPotionEffectsOnly", true);
        builder.push("upgradeCostCoefficients");
        EARTHEN_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "earthen", 6);
        AQUEOUS_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "aqueous", 6);
        ARCANE_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "arcane", 6);
        AERIAL_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "aerial", 1);
        SACRED_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "sacred", 1);
        INFERNAL_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "infernal", 6);
        ELDRITCH_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "eldritch", 1);
        WICKED_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "wicked", 1);
        builder.pop();
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private static ForgeConfigSpec.IntValue defineUpgradeCostCoefficient(
            ForgeConfigSpec.Builder builder, String spirit, int defaultValue) {
        return builder
                .comment("Upgrade cost coefficient for the " + spirit + " spirit. Must be at least 1.")
                .defineInRange(spirit, defaultValue, 1, Integer.MAX_VALUE);
    }

    private MaledictConfig() {
    }
}
