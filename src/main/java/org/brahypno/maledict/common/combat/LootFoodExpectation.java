package org.brahypno.maledict.common.combat;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import org.brahypno.changelib.LootHelper.EntityLootScanner;
import org.brahypno.changelib.LootHelper.LootScanCommon.LootCandidate;
import org.brahypno.changelib.LootHelper.LootTableItemScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How much hunger and saturation a living entity's drops are expected to provide, plus the mob
 * effects the food among them carries.
 * <p>
 * The changelib scanners report, for every entry of every loot source, how likely the entry is to
 * be reached and how many items it hands out. Expectation is linear, so summing
 * {@code reach * expectedCount * value} over the entries is the expected nourishment of one kill
 * rather than a guess. Nothing is rolled here: the result is a value, not an item.
 * <p>
 * {@code hungerPerItem} and {@code saturationPerItem} are the coefficients of the enchantment's
 * "one extra item per level" bonus. That bonus is part of the same expectation, so it is weighted
 * by the reach chance as well and can only ever pay out on food the victim would really drop.
 * {@link EffectSource} keeps the same two numbers for the food that carries effects, because
 * tasting a meal means its aftertaste comes with it.
 */
public record LootFoodExpectation(
        double hunger,
        double hungerPerItem,
        double saturation,
        double saturationPerItem,
        List<EffectSource> effectSources) {

    public static final LootFoodExpectation NONE =
            new LootFoodExpectation(0.0D, 0.0D, 0.0D, 0.0D, List.of());

    private static final int MAX_CACHE_ENTRIES = 512;
    private static final Map<Key, LootFoodExpectation> CACHE = new ConcurrentHashMap<>();

    /**
     * One food entry that carries mob effects, with the expectation of how many of it the victim
     * would have handed over. Vanilla rolls eating effects once per item, but repeated applications
     * of the same effect only refresh its duration, so the consumer can roll once at the
     * at-least-once chance and get the same outcome.
     */
    public record EffectSource(
            double reach,
            double expectedCount,
            List<Pair<MobEffectInstance, Float>> effects) {

        public EffectSource {
            effects = List.copyOf(effects);
        }

        /** Expected number of this item a kill is worth, with the per level bonus included. */
        public double servingsAt(int level) {
            return reach * (expectedCount + level);
        }
    }

    /**
     * The expectation for this victim, resolved from everything it can drop and cached per loot
     * table, entity type, looting level and fire state - the fire state matters because a burning
     * victim smelts its meat on death. Only {@link #clearCache()} invalidates the entries, so a
     * datapack reload has to call it.
     */
    public static LootFoodExpectation of(ServerLevel level, LivingEntity victim, int lootingLevel) {
        ResourceLocation lootTable = victim.getLootTable();
        if (lootTable == null){
            return NONE;
        }

        boolean onFire = victim.isOnFire();
        Key key = new Key(victim.getType(), lootTable, lootingLevel, onFire);
        LootFoodExpectation cached = CACHE.get(key);
        if (cached != null){
            return cached;
        }

        LootFoodExpectation scanned = scan(level, victim, lootingLevel, onFire);
        if (CACHE.size() >= MAX_CACHE_ENTRIES){
            CACHE.clear();
        }
        CACHE.put(key, scanned);
        return scanned;
    }

    /** Drops are data driven, so every cached expectation dies with the resource reload. */
    public static void clearCache() {
        CACHE.clear();
    }

    public double hungerAt(int level) {
        return hunger + hungerPerItem * level;
    }

    public double saturationAt(int level) {
        return saturation + saturationPerItem * level;
    }

    private static LootFoodExpectation scan(
            ServerLevel level, LivingEntity victim, int lootingLevel, boolean onFire) {
        double hunger = 0.0D;
        double hungerPerItem = 0.0D;
        double saturation = 0.0D;
        double saturationPerItem = 0.0D;
        List<EffectSource> effectSources = new ArrayList<>();

        LootTableItemScanner.LootScanOptions options =
                LootTableItemScanner.LootScanOptions.looting(lootingLevel).withOnFire(onFire);

        for (LootCandidate candidate : EntityLootScanner.collectAll(level, victim, options)) {
            if (candidate.countRange().max() <= 0){
                continue;
            }

            FoodProperties food = candidate.stack().getFoodProperties(null);
            if (food == null){
                continue;
            }

            int nutrition = food.getNutrition();
            double itemSaturation = food.getSaturationModifier() * nutrition * 2.0D;
            List<Pair<MobEffectInstance, Float>> effects = effectsOf(food);
            if (nutrition <= 0 && itemSaturation <= 0.0D && effects.isEmpty()){
                continue;
            }

            double reach = candidate.reachRate();
            hunger += candidate.expectedYield() * nutrition;
            hungerPerItem += reach * nutrition;
            saturation += candidate.expectedYield() * itemSaturation;
            saturationPerItem += reach * itemSaturation;

            if (!effects.isEmpty()){
                effectSources.add(new EffectSource(reach, candidate.countRange().expected(), effects));
            }
        }

        if (hunger <= 0.0D && saturation <= 0.0D && hungerPerItem <= 0.0D && saturationPerItem <= 0.0D
            && effectSources.isEmpty()){
            return NONE;
        }
        return new LootFoodExpectation(hunger, hungerPerItem, saturation, saturationPerItem, effectSources);
    }

    /**
     * The effects eating this food would apply, with the entries vanilla skips already dropped.
     * The instances stay shared with the item's food properties - the consumer copies them, the
     * same way vanilla does.
     */
    private static List<Pair<MobEffectInstance, Float>> effectsOf(FoodProperties food) {
        List<Pair<MobEffectInstance, Float>> effects = new ArrayList<>();
        for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
            if (effect.getFirst() != null){
                effects.add(effect);
            }
        }
        return effects;
    }

    private record Key(EntityType<?> type, ResourceLocation lootTable, int lootingLevel, boolean onFire) {
    }
}
