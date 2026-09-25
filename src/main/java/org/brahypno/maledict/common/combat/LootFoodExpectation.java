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
 * effects the food among them carries. Nothing is rolled here: the result is a value, not an item.
 * <p>{@code hungerPerItem} and {@code saturationPerItem} are the coefficients of the enchantment's
 * "one extra item per level" bonus.
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
     * One food entry that carries mob effects. Vanilla rolls eating effects once per item, but
     * repeated applications only refresh the duration, so one roll at the at-least-once chance matches.
     */
    public record EffectSource(
            double reach,
            double expectedCount,
            List<Pair<MobEffectInstance, Float>> effects) {

        public EffectSource {
            effects = List.copyOf(effects);
        }

        /** Expected servings, with the per level bonus included. */
        public double servingsAt(int level) {
            return reach * (expectedCount + level);
        }
    }

    /**
     * Cached per loot table, entity type, looting level and fire state - the fire state matters
     * because a burning victim smelts its meat. A datapack reload must call {@link #clearCache()}.
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
     * The effects eating this food would apply, vanilla-skipped entries dropped. The instances stay
     * shared with the item's food properties - the consumer copies them.
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
