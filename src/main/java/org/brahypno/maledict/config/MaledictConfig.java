package org.brahypno.maledict.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MaledictConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue SCREENSHAKE_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue VICISSITUDE_ENGAGEMENT_RANGE;
    public static final ForgeConfigSpec.BooleanValue AERIAL_POTION_EFFECTS_ONLY;
    public static final ForgeConfigSpec.DoubleValue EARTHEN_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue AQUEOUS_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue ARCANE_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue AERIAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue SACRED_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue INFERNAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue ELDRITCH_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue WICKED_UPGRADE_COST_COEFFICIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("incursusBlade");
        AERIAL_POTION_EFFECTS_ONLY = builder
                .comment("When true, Aerial Spirit can only grant beneficial effects used by registered potions.")
                .define("aerialPotionEffectsOnly", true);
        builder.push("upgradeCostCoefficients");
        EARTHEN_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "earthen", 3);
        AQUEOUS_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "aqueous", 3);
        ARCANE_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "arcane", 3);
        AERIAL_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "aerial", 1);
        SACRED_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "sacred", 1);
        INFERNAL_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "infernal", 6);
        ELDRITCH_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "eldritch", 1);
        WICKED_UPGRADE_COST_COEFFICIENT = defineUpgradeCostCoefficient(builder, "wicked", 0.5);
        builder.pop();
        builder.push("firstVicissitude");
        VICISSITUDE_ENGAGEMENT_RANGE = builder
                .comment("How far, in blocks, the First Vicissitude picks up and keeps a target.",
                        "This is the boss' real follow range: beyond it the encounter lets you go,",
                        "which is the vanilla style death forgiveness behaviour. It has to stay",
                        "above the 64 block no-fire range, otherwise the boss would drop the target",
                        "before the approach-without-firing behaviour it is built around can happen.",
                        "Lower it to make the boss less willing to cross the arena towards you.")
                .defineInRange("engagementRange", 96.0D, 4.0D, 256.0D);
        builder.pop();
        builder.pop();
        COMMON_SPEC = builder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.push("firstVicissitude");
        SCREENSHAKE_INTENSITY = clientBuilder
                .comment("Client side multiplier for the First Vicissitude screenshake. 0 disables it.")
                .defineInRange("screenshakeIntensity", 1.0D, 0.0D, 1.0D);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    private static ForgeConfigSpec.DoubleValue defineUpgradeCostCoefficient(ForgeConfigSpec.Builder builder, String spirit, double defaultValue) {
        return builder
                .comment("Upgrade cost coefficient for the " + spirit + " spirit. Must be at least 1.")
                .defineInRange(spirit, defaultValue, 0.5, Integer.MAX_VALUE);
    }

    private MaledictConfig() {
    }
}
