package org.brahypno.maledict.common.combat;

import com.mojang.datafixers.util.Pair;
import com.sammy.malum.registry.common.DamageTypeTagRegistry;
import com.sammy.malum.registry.common.item.ItemTagRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.enchantment.AftertasteEnchantment;
import org.brahypno.maledict.registry.MaledictEnchantments;

/**
 * Feeds a scythe's wielder with whatever the victim's corpse was going to feed them; the drop list
 * is only a value here, so nothing is spawned, removed or rolled.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AftertasteEnchantmentEffect {
    /** Fraction of a hunger point carried over until it is worth a whole point. */
    private static final String HUNGER_CARRY_TAG = Maledict.MODID + ":aftertaste_hunger_carry";
    private static final int MAX_HUNGER = 20;

    @SubscribeEvent
    public static void restoreFromDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getAmount() <= 0.0F || !(victim.level() instanceof ServerLevel level)){
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || player == victim){
            return;
        }
        if (!isScytheSwing(source, player)){
            return;
        }

        ItemStack scythe = player.getMainHandItem();
        int enchantmentLevel = scythe.is(ItemTagRegistry.SCYTHE)
                               ? scythe.getEnchantmentLevel(MaledictEnchantments.AFTERTASTE.get())
                               : 0;
        if (enchantmentLevel <= 0){
            return;
        }

        float maxHealth = victim.getMaxHealth();
        if (maxHealth <= 0.0F){
            return;
        }

        // Only the share of the corpse this hit claimed is tasted, so overkill cannot pay out more
        // than the whole creature.
        double claimedShare = Mth.clamp(event.getAmount() / (double) maxHealth, 0.0D, 1.0D);
        double restoreShare = AftertasteEnchantment.getRestoreShare(enchantmentLevel) * claimedShare;

        LootFoodExpectation expectation = LootFoodExpectation.of(
                level, victim, ForgeHooks.getLootingLevel(victim, player, source));
        double hunger = expectation.hungerAt(enchantmentLevel) * restoreShare;
        double saturation = expectation.saturationAt(enchantmentLevel) * restoreShare;
        if (hunger <= 0.0D && saturation <= 0.0D && expectation.effectSources().isEmpty()){
            return;
        }

        feed(player, hunger, saturation);
        applyFoodEffects(player, expectation, enchantmentLevel, restoreShare);
    }

    /**
     * The direct entity has to be the wielder (arrows, owned explosions and thorns retaliation all
     * carry the wielder as the causing entity too), or the type has to be one Malum builds from a
     * damage type that names no direct entity at all.
     */
    private static boolean isScytheSwing(DamageSource source, Player player) {
        if (source.is(DamageTypes.THORNS)){
            return false;
        }
        Entity direct = source.getDirectEntity();
        return direct == player || source.is(DamageTypeTagRegistry.IS_SCYTHE);
    }

    /**
     * Expectation is fractional while hunger is whole, so the leftover is carried on the player
     * until it adds up; rounding each hit would delete every gain below a whole point.
     */
    private static void feed(Player player, double hunger, double saturation) {
        CompoundTag data = player.getPersistentData();
        double carried = data.getDouble(HUNGER_CARRY_TAG) + hunger;
        int wholeHunger = (int) Math.floor(carried);
        data.putDouble(HUNGER_CARRY_TAG, carried - wholeHunger);

        FoodData food = player.getFoodData();
        if (wholeHunger > 0){
            food.setFoodLevel(Math.min(MAX_HUNGER, food.getFoodLevel() + wholeHunger));
        }
        food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + (float) saturation));
    }

    /**
     * Vanilla rolls once per eaten item, but applying the same effect again only refreshes its
     * duration, so a single roll at the at-least-once chance gives the same outcome.
     */
    private static void applyFoodEffects(
            Player player,
            LootFoodExpectation expectation,
            int level,
            double tastedShare) {
        for (LootFoodExpectation.EffectSource source : expectation.effectSources()) {
            double servings = source.servingsAt(level) * tastedShare;
            if (servings <= 0.0D){
                continue;
            }

            for (Pair<MobEffectInstance, Float> effect : source.effects()) {
                double chance = Mth.clamp(effect.getSecond(), 0.0D, 1.0D);
                if (player.getRandom().nextFloat() < 1.0D - Math.pow(1.0D - chance, servings)){
                    player.addEffect(new MobEffectInstance(effect.getFirst()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        LootFoodExpectation.clearCache();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LootFoodExpectation.clearCache();
    }

    private AftertasteEnchantmentEffect() {
    }
}
