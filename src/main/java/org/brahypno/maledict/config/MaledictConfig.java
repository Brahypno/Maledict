package org.brahypno.maledict.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MaledictConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue SCREENSHAKE_INTENSITY;
    public static final ForgeConfigSpec.BooleanValue VICISSITUDE_BOSS_MUSIC;
    public static final ForgeConfigSpec.DoubleValue VICISSITUDE_BOSS_MUSIC_VOLUME;
    public static final ForgeConfigSpec.DoubleValue VICISSITUDE_ENGAGEMENT_RANGE;
    public static final ForgeConfigSpec.DoubleValue VICISSITUDE_DAMAGE_RANGE;
    public static final ForgeConfigSpec.IntValue VICISSITUDE_ADAPTATION_LEVEL;
    public static final ForgeConfigSpec.BooleanValue SUMMONING_RITE;
    public static final ForgeConfigSpec.ConfigValue<String> SUMMONING_RITE_ENTITY;
    public static final ForgeConfigSpec.BooleanValue AERIAL_POTION_EFFECTS_ONLY;
    public static final ForgeConfigSpec.DoubleValue EARTHEN_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue AQUEOUS_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue ARCANE_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue AERIAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue SACRED_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue INFERNAL_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue ELDRITCH_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue WICKED_UPGRADE_COST_COEFFICIENT;
    public static final ForgeConfigSpec.DoubleValue ENLIGHTENMENT_COOLDOWN_SPEED;
    public static final ForgeConfigSpec.DoubleValue ENLIGHTENMENT_MELEE_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue INFRARED_ENABLED;
    public static final ForgeConfigSpec.DoubleValue INFRARED_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue INFRARED_GRAYSCALE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue INFRARED_GRAYSCALE_SOFTNESS;

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
        builder.push("ageOfEnlightenment");
        ENLIGHTENMENT_COOLDOWN_SPEED = builder
                .comment("How much faster item cooldowns recover while the Age of Enlightenment is worn.",
                        "2.0 means cooldowns run at double speed: the wearer's active cooldowns lose an",
                        "extra tick every server tick. This is a passive of the curio itself, not tied",
                        "to the half health trigger or to kills. Values of 1.0 or below mean no boost.",
                        "Note that the vanilla client only receives a cooldown once and then counts it",
                        "down on its own, so the cooldown sweep can look slower than it really is; the",
                        "cooldown does end when the server says it does.")
                .defineInRange("cooldownSpeed", 2.0D, 1.0D, 20.0D);
        ENLIGHTENMENT_MELEE_DAMAGE_MULTIPLIER = builder
                .comment("Damage multiplier for the wearer's melee hits while the Age of Enlightenment",
                        "is active. 2.0 doubles the pre-armour damage of anything the wearer's own",
                        "body deals - vanilla attacks, the Incursus Blade's own swing and its sweep",
                        "splash - while magic, arrows and explosions keep their own numbers. A real",
                        "vanilla critical hit (the falling attack) still multiplies by 1.5 first",
                        "and is then doubled by this. 1.0 turns the bonus off.")
                .defineInRange("meleeDamageMultiplier", 2.0D, 1.0D, 100.0D);
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
        VICISSITUDE_DAMAGE_RANGE = builder
                .comment("How far, in blocks, the First Vicissitude takes damage at full strength.",
                        "Beyond it damage falls off linearly and reaches zero at 1.5 times this",
                        "value, so 48 means full damage to 48 blocks and nothing to hit it with",
                        "past 72. Damage with no living attacker - environment, commands, falls -",
                        "ignores this. Keep it above the 7 block distance the boss likes to hold,",
                        "otherwise its own spacing would blunt every melee hit it takes.")
                .defineInRange("damageRange", 48.0D, 4.0D, 128.0D);
        VICISSITUDE_ADAPTATION_LEVEL = builder
                .comment("Which adaptation this boss is - the number of damage messages it can",
                        "record at once. 2 is the authored 'adaptation two'. The boss records the",
                        "damage message (the id behind the death message, such as 'player',",
                        "'arrow' or 'scythe_sweep') of every hit it takes, and a message it has",
                        "already recorded deals exponentially less damage - the second hit is",
                        "e^-1, the third e^-2 and so on. The record has no timer: it is per",
                        "encounter and only wiped when the boss goes back to being dormant. Once",
                        "every slot is full, a new message replaces the oldest record, so rotating",
                        "through three or more damage types stays at full damage. 0 turns the whole",
                        "adaptation off.")
                .defineInRange("adaptationLevel", 2, 0, 16);
        SUMMONING_RITE = builder
                .comment("When true, the Vicissitude Rites - spirit recipes no other totemic rite",
                        "uses - do what their page promises and create something above the totem.",
                        "Both are one time effects, so a soulwood totem fires them once as well and",
                        "never spawns a crowd. The recipe decides the difficulty of what answers:",
                        "three arcane spirits bring the standard encounter, and the eldritch",
                        "recipe (two eldritch over two arcane) brings the extreme one.")
                .define("summoningRite", true);
        SUMMONING_RITE_ENTITY = builder
                .comment("Registry name of the entity the Vicissitude Rites create.")
                .define("summoningRiteEntity", "maledict:first_vicissitude");
        builder.pop();
        builder.pop();
        COMMON_SPEC = builder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.push("firstVicissitude");
        SCREENSHAKE_INTENSITY = clientBuilder
                .comment("Client side multiplier for the First Vicissitude screenshake. 0 disables it.")
                .defineInRange("screenshakeIntensity", 1.0D, 0.0D, 1.0D);
        VICISSITUDE_BOSS_MUSIC = clientBuilder
                .comment("When true, the First Vicissitude gets its own battle theme: one for phase",
                        "one, one for phase two. Turning this off means no music from this mod",
                        "during the encounter at all.",
                        "The boss bar is unaffected either way.")
                .define("bossMusic", true);
        VICISSITUDE_BOSS_MUSIC_VOLUME = clientBuilder
                .comment("Volume multiplier for the encounter's music, 1.0 being as authored.",
                        "Vanilla's Music slider still applies on top of this, and setting that",
                        "slider to 0 mutes the theme on its own.")
                .defineInRange("bossMusicVolume", 1.0D, 0.0D, 1.0D);
        clientBuilder.pop();
        clientBuilder.push("infrared");
        INFRARED_ENABLED = clientBuilder
                .comment("When true, the eye slot (the 'delusion' slot earned from Wisdom's",
                        "Sacrifice) works as a low-light optical device. With something in that",
                        "slot and its eye toggle lit, the client fades in a grayscale low-light",
                        "view whenever the camera sits in darkness, the wearer is blind or",
                        "wrapped in darkness, or the camera is inside lava - and pushes the fog",
                        "walls those things rely on out of the way. Purely visual and client",
                        "side: no world lighting, no mob effects, nothing sent to the server.",
                        "Turn this off to disable the whole feature.")
                .define("enabled", true);
        INFRARED_INTENSITY = clientBuilder
                .comment("How far the low-light view fades in once every condition is met.",
                        "1.0 is as authored - the far end of the fade. Lower values keep the",
                        "effect as a lighter wash over the normal picture, and 0.0 means the",
                        "fade never gets anywhere. The fade itself takes about four tenths of",
                        "a second either way.")
                .defineInRange("intensity", 1.0D, 0.0D, 1.0D);
        INFRARED_GRAYSCALE_THRESHOLD = clientBuilder
                .comment("Dynamic grayscale: pixels dimmer than this lose all of their colour,",
                        "pixels above it keep theirs. 0.30 keeps the near, dark parts of the",
                        "scene in infrared grey while lit surfaces, lava and the sky stay",
                        "coloured. Raise it towards 1.0 to grey the whole picture out, lower it",
                        "to leave more colour alone.")
                .defineInRange("grayscaleThreshold", 0.30D, 0.0D, 1.0D);
        INFRARED_GRAYSCALE_SOFTNESS = clientBuilder
                .comment("How wide the band above grayscaleThreshold is, in the same 0..1",
                        "luminance units. Colour fades back in across it, so 0.0 gives a hard",
                        "edge between grey and colour while 0.30 gives a soft one. Setting this",
                        "and the threshold to 0 together leaves the picture untouched, since",
                        "nothing is dim enough to be caught.")
                .defineInRange("grayscaleSoftness", 0.30D, 0.0D, 1.0D);
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
