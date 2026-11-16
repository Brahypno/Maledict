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
    public static final ForgeConfigSpec.DoubleValue LIGHTWARD_ABNEGATION_REDUCTION;
    public static final ForgeConfigSpec.IntValue SOULBINDING_SPIRIT_SOUND_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue INFRARED_ENABLED;
    public static final ForgeConfigSpec.DoubleValue INFRARED_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue INFRARED_GRAYSCALE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue INFRARED_GRAYSCALE_SOFTNESS;
    public static final ForgeConfigSpec.DoubleValue HANGMAN_REFLECTION_BASE;
    public static final ForgeConfigSpec.IntValue HANGMAN_TRAP_LIFETIME;
    public static final ForgeConfigSpec.DoubleValue HANGMAN_TRAP_RADIUS;
    public static final ForgeConfigSpec.IntValue EMPEROR_EGGSHELL_WINDOW_TICKS;
    public static final ForgeConfigSpec.BooleanValue LUMENFALL_SPREAD_RADIANCE;
    public static final ForgeConfigSpec.IntValue LUMENFALL_SPREAD_RADIANCE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue LUMENFALL_SPREAD_RADIANCE_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue LUMENFALL_RELEASE_RADIANCE_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue LUMENFALL_RELEASE_RADIANCE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue OMEN_MINIMUM_SHARE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_CHARGE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_LASER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_RAIN_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_SWEEP_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_PILLAR_RISE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_PILLAR_EXPLOSION_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_PILLAR_EMPOWERED_DAMAGE;
    public static final ForgeConfigSpec.IntValue DEMIURGE_PILLAR_HEALTH;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_EASY_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_NORMAL_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEMIURGE_HARD_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue INCURSUS_RIFT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue INCURSUS_RIFT_SHADER;
    public static final ForgeConfigSpec.IntValue INCURSUS_RIFT_LIFETIME;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_FADE_START;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_PETAL_SIZE;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_RADIAL_SPEED;
    public static final ForgeConfigSpec.IntValue INCURSUS_RIFT_PETALS;
    public static final ForgeConfigSpec.IntValue INCURSUS_RIFT_MAX_INSTANCES;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_WIDEN;
    public static final ForgeConfigSpec.DoubleValue INCURSUS_RIFT_STARS;
    public static final ForgeConfigSpec.BooleanValue INCURSUS_RIFT_DEPTH_TEST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("demiurge");
        DEMIURGE_CHARGE_DAMAGE = builder.comment("Base charge damage before the world difficulty multiplier.")
                                        .defineInRange("chargeDamage", 7.0D, 0.0D, 10000.0D);
        DEMIURGE_LASER_DAMAGE = builder.comment("Base damage per laser pulse before the world difficulty multiplier.")
                                       .defineInRange("laserDamage", 6.0D, 0.0D, 10000.0D);
        DEMIURGE_RAIN_DAMAGE = builder.comment("Base damage per tear before the world difficulty multiplier.")
                                      .defineInRange("rainDamage", 8.0D, 0.0D, 10000.0D);
        DEMIURGE_SWEEP_DAMAGE = builder.comment("Base damage of one serpent sweep before the world difficulty multiplier.")
                                       .defineInRange("sweepDamage", 10.0D, 0.0D, 10000.0D);
        DEMIURGE_PILLAR_RISE_DAMAGE = builder.comment("Base damage when a False Creation pillar rises through a target.")
                                             .defineInRange("pillarRiseDamage", 8.0D, 0.0D, 10000.0D);
        DEMIURGE_PILLAR_EXPLOSION_DAMAGE = builder.comment("Base damage of a pillar's normal timed explosion.")
                                                  .defineInRange("pillarExplosionDamage", 8.0D, 0.0D, 10000.0D);
        DEMIURGE_PILLAR_EMPOWERED_DAMAGE = builder.comment("Base damage of a sweep-triggered empowered pillar explosion.")
                                                  .defineInRange("pillarEmpoweredDamage", 12.0D, 0.0D, 10000.0D);
        // Durability was left open in the design. 24 lets an iron sword break a
        // pillar in four full hits and a netherite sword in three, so clearing a
        // pillar is a real but affordable alternative to using it as cover.
        DEMIURGE_PILLAR_HEALTH = builder.comment("Health of one False Creation pillar; destroyed by player damage only.")
                                        .defineInRange("pillarHealth", 24, 1, 10000);
        DEMIURGE_EASY_DAMAGE_MULTIPLIER = builder.defineInRange("easyDamageMultiplier", 0.5D, 0.0D, 100.0D);
        DEMIURGE_NORMAL_DAMAGE_MULTIPLIER = builder.defineInRange("normalDamageMultiplier", 1.0D, 0.0D, 100.0D);
        DEMIURGE_HARD_DAMAGE_MULTIPLIER = builder.defineInRange("hardDamageMultiplier", 1.5D, 0.0D, 100.0D);
        builder.pop();
        builder.push("transcendOmen");
        OMEN_MINIMUM_SHARE = builder.comment("Minimum share per active reverse route. Total shares may exceed 100%.")
                                    .defineInRange("minimumShare", 0.01D, 0.0D, 1.0D);
        builder.pop();
        builder.push("hangmanFool");
        HANGMAN_REFLECTION_BASE = builder.comment("Base reflection chance, plus lost health fraction, capped at 100%.")
                                         .defineInRange("reflectionBaseChance", 0.25D, 0.0D, 1.0D);
        HANGMAN_TRAP_LIFETIME = builder.comment("Trap lifetime in ticks; 600 ticks is 30 seconds.")
                                       .defineInRange("trapLifetime", 600, 1, 72000);
        HANGMAN_TRAP_RADIUS = builder.comment("Contact distance in blocks around the impact point.")
                                     .defineInRange("trapRadius", 1.0D, 0.1D, 16.0D);
        builder.pop();
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
        builder.push("aufhebenLightward");
        LIGHTWARD_ABNEGATION_REDUCTION = builder
                .comment("The 3 piece effect, Soul over Flesh: while the wearer still has soul ward,",
                         "this share of the final damage is absorbed once more. It runs on",
                         "LivingDamageEvent, so the amount is what is left after armour, protection",
                         "enchantments and absorption - the damage that would leave the health bar.",
                         "Malum's own soul ward already ran earlier, on LivingHurtEvent; its 30%",
                         "physical / 90% magic split is deliberately not repeated here, this is one",
                         "flat share for every damage type. The absorbed share is paid out of the",
                         "soul ward, divided by Soul Ward Integrity, exactly like Malum's own",
                         "absorption. 0.0 turns the effect off, 1.0 absorbs everything.")
                .defineInRange("abnegationReduction", 0.25D, 0.0D, 1.0D);
        builder.pop();
        builder.push("soulbinding");
        SOULBINDING_SPIRIT_SOUND_COOLDOWN = builder
                .comment("Ticks during which a bound tool's spirit sound is not played again.",
                         "The sound is only extra feedback on a hit that was really dealt: one",
                         "swing can arrive as several hurts (the strike, sweep damage and the",
                         "arcane bonus), and the cooldown folds them into one sound. It never",
                         "suppresses damage, and 0 plays it for every hurt event.")
                .defineInRange("spiritSoundCooldown", 10, 0, 200);
        builder.pop();
        builder.push("emperorInTheEggshell");
        EMPEROR_EGGSHELL_WINDOW_TICKS = builder
                .comment("How long the Eggshell Emperor's damage window lasts after a hit, in ticks.",
                         "The opening hit (and every other hit in its tick) is full damage; after",
                         "that, incoming damage is scaled by elapsed / windowTicks. At 40, a hit",
                         "20 ticks later takes 50% damage and a hit at 40 ticks is full again.",
                         "Vanilla's own 20-tick i-frame window stays in front of this one, so",
                         "values at or below 20 have no visible effect; the minimum is 21.",
                         "Same-tick hits count once, at the largest raw amount.")
                .defineInRange("windowTicks", 40, 21, 200);
        builder.pop();
        builder.push("lumenfall");
        LUMENFALL_SPREAD_RADIANCE = builder
                .comment("Whether a Lumenfall death paints the big radiance burst at the death spot.",
                         "This reuses Malum's weeping-well radiance effect, which has no size, count or",
                         "intensity parameter - one dispatch is as loud as the effect itself. So it is",
                         "weakened by conditions instead: it only plays when the spread actually caught",
                         "someone else, and at most once per dimension per cooldown below. Turn it off",
                         "to keep only the per-target light specks.")
                .define("spreadRadiance", true);
        LUMENFALL_SPREAD_RADIANCE_COOLDOWN_TICKS = builder
                .comment("Minimum ticks between two radiance bursts in the same dimension. 20 means at",
                         "most one per second, so a row of kills cannot stack the big effect. 0 removes",
                         "the throttle (not recommended with the burst enabled).")
                .defineInRange("spreadRadianceCooldownTicks", 20, 0, 400);
        LUMENFALL_SPREAD_RADIANCE_STRENGTH = builder
                .comment("Strength of the radiance burst, 0.05 to 1.0. This is Maledict's own copy of",
                         "Malum's weeping-well radiance (see LumenfallRadianceActor), so unlike the stock",
                         "effect it scales: 1.0 is the full original burst, 0.5 halves the emitted",
                         "bursts and shrinks the central sparkles, lower values fade to nothing.")
                .defineInRange("spreadRadianceStrength", 0.5D, 0.05D, 1.0D);
        LUMENFALL_RELEASE_RADIANCE_STRENGTH = builder
                .comment("Strength of the smaller radiance burst painted on the target when the 80 tick",
                         "release fires without a kill - the single-target version of the death burst,",
                         "same effect type at a much lower intensity. This replaces the old per-target",
                         "sparkle layer rather than adding to it. 0 turns the release visual off.")
                .defineInRange("releaseRadianceStrength", 0.2D, 0.0D, 1.0D);
        LUMENFALL_RELEASE_RADIANCE_RADIUS = builder
                .comment("How far the release burst spreads, as a multiplier of the full death-burst",
                         "extent. Strength above controls how many particles there are; this controls",
                         "how wide they fly. The release is single target, so it must not fan out like",
                         "the death spread: 0.35 keeps it a tight puff on the victim. Values below 0.9",
                         "also drop the big central square flashes (fixed-size Malum pieces that only",
                         "belong to the full-size burst).")
                .defineInRange("releaseRadianceRadius", 0.35D, 0.1D, 1.0D);
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
                .comment("When true, the Vision slot ( earned from Wisdom's Sacrifice)",
                         " works as a low-light optical device. With the slot's eye toggle lit,",
                         "the client fades in a grayscale low-light view only when normal vision",
                         "is too dark and night vision or a gamma override does not already help.",
                         "Obscuring effect and fluid fog is removed independently, including",
                         "when normal vision is already bright enough. Purely visual and client",
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
        clientBuilder.push("incursusRift");
        INCURSUS_RIFT_ENABLED = clientBuilder
                .comment("When true, the Incursus Blade's swing paints the Rift Bloom instead of the old",
                         "Malum slash particles: the same eight crossed blade strokes, but each one is a",
                         "cut into a starless void with a broken silver violet edge.",
                         "Purely visual and client side. Turning this off restores the old particles.")
                .define("enabled", true);
        INCURSUS_RIFT_SHADER = clientBuilder
                .comment("Whether the rift uses its own core shader (void body and star field).",
                         "With this off the same mask and geometry are drawn with the vanilla",
                         "position_tex_color shader in a flat near black, which is the fallback",
                         "when the shader fails to load or another rendering mod breaks it.")
                .define("shader", true);
        INCURSUS_RIFT_LIFETIME = clientBuilder
                .comment("How long one swing's rift lasts, in ticks. The old effect was 5 ticks of",
                         "Malum's heavy slash at the project's 2.5x lifetime multiplier, so 12 ticks",
                         "is the rhythm the current flower has; raise it for a slower unfold.")
                .defineInRange("lifetimeTicks", 12, 4, 60);
        INCURSUS_RIFT_FADE_START = clientBuilder
                .comment("Where the fade out begins, as a share of the lifetime. The old particles were",
                         "fully opaque the whole time and simply vanished at the end, so 0.95 is the",
                         "faithful default. Lower it for an earlier, softer fade.")
                .defineInRange("fadeStart", 0.95D, 0.10D, 0.95D);
        INCURSUS_RIFT_INTENSITY = clientBuilder
                .comment("Overall opacity multiplier, 1.0 as authored. Lower it to make the rift a",
                         "lighter wash over the scene, raise it towards 2.0 for a denser black.")
                .defineInRange("intensity", 1.0D, 0.0D, 2.0D);
        INCURSUS_RIFT_PETAL_SIZE = clientBuilder
                .comment("Half extent of one blade stroke's quad, in blocks, before the swing's own",
                         "scale multiplier. The old particles used 3.0 to 4.5 blocks here, because the",
                         "hand drawn stroke only covers a small part of its 64x64 texture; shrinking",
                         "this number shrinks the drawn stroke with it. 3.75 is the old midpoint.")
                .defineInRange("petalSize", 3.75D, 0.5D, 12.0D);
        INCURSUS_RIFT_RADIAL_SPEED = clientBuilder
                .comment("Outward travel of each petal in blocks per tick, before the swing's scale",
                         "multiplier. The old particles drifted 0.3 to 0.4 blocks per tick for their",
                         "whole 12 tick life, so 0.35 reproduces the same opening.")
                .defineInRange("radialSpeed", 0.35D, 0.0D, 2.0D);
        INCURSUS_RIFT_PETALS = clientBuilder
                .comment("How many blade strokes form the flower. 8 matches the old ring exactly;",
                         "lower values are cheaper and read less like a flower, higher values fill the",
                         "ring more densely.")
                .defineInRange("petals", 8, 4, 16);
        INCURSUS_RIFT_MAX_INSTANCES = clientBuilder
                .comment("How many rifts may be alive at once. Fast successive swings overlap up to",
                         "this many; the oldest is dropped first.")
                .defineInRange("maxInstances", 6, 1, 32);
        INCURSUS_RIFT_WIDEN = clientBuilder
                .comment("How far the shader widens the hairline stroke, in texture UV. The quad is",
                         "roughly 7.5 blocks wide, so one UV unit spans all of it: 0.10 turns every",
                         "stroke into a three quarter block halo and eight of them merge into one",
                         "slab, while 0.006 is about half a texture texel. 0 keeps exactly the",
                         "original knife mark. This no longer adds a coloured outline; it only",
                         "decides how wide the dark slit around the night sky is.")
                .defineInRange("widen", 0.006D, 0.0D, 0.30D);
        INCURSUS_RIFT_STARS = clientBuilder
                .comment("Star field strength inside the void, 1.0 as authored. 0 leaves a pure black",
                         "cut with no stars at all.")
                .defineInRange("stars", 1.0D, 0.0D, 3.0D);
        INCURSUS_RIFT_DEPTH_TEST = clientBuilder
                .comment("Whether the rift is hidden by terrain and entities in front of it.",
                         "The old slash particles used Malum's noClip particles, so the flower drew",
                         "straight through walls; false keeps that behaviour and is the faithful",
                         "default. Turn this on to make the rift respect world depth instead.")
                .define("respectWorldDepth", false);
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
