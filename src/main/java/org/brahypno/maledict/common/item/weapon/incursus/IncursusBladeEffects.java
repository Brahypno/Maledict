package org.brahypno.maledict.common.item.weapon.incursus;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;
import team.lodestar.lodestone.helpers.EntityHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Maledict.MODID)
public final class IncursusBladeEffects {
    @SubscribeEvent
    public static void applyWickedCriticalDamage(CriticalHitEvent event) {
        ItemStack weapon = event.getEntity().getMainHandItem();
        if (!(weapon.getItem() instanceof IncursusBladeItem)){
            return;
        }
        float bonus = (float) IncursusBladeItem.getStat(weapon, IncursusBladeItem.WICKED_CRITICAL_DAMAGE) / 100.0f;
        event.setDamageMultiplier(event.getDamageMultiplier() + bonus);
    }

    @SubscribeEvent
    public static void applyInfernalLooting(GetEnchantmentLevelEvent event) {
        if (!event.isTargetting(Enchantments.LOOTING)) return;
        ItemStack weapon = event.getStack();
        if (!(weapon.getItem() instanceof IncursusBladeItem)) return;
        int bonus = Math.max(0, (int) Math.floor(IncursusBladeItem.getStat(
                weapon, IncursusBladeItem.INFERNAL_POWER)));
        event.getHolder(Enchantments.LOOTING).ifPresent(looting ->
                event.getEnchantments().set(looting, event.getEnchantments().getLevel(looting) + bonus));
    }

    public static void applyAerialEffect(ServerPlayer player, ItemStack weapon) {
        double progress = Math.max(0.0, IncursusBladeItem.getStat(
                weapon, IncursusBladeItem.AERIAL_PROGRESS));
        EffectScaling scaling = getEffectScaling(player, progress);
        if (scaling == null){
            return;
        }

        removeRandomNegativeEffects(player, scaling.effectCount);
        grantRandomPositiveEffects(
                player, scaling.effectCount, scaling.durationSteps * 200, scaling.amplifier);
    }

    public static void applySacredEffect(ServerPlayer player, LivingEntity target, ItemStack weapon) {
        double progress = Math.max(0.0, IncursusBladeItem.getStat(
                weapon, IncursusBladeItem.SACRED_POWER));
        EffectScaling scaling = getEffectScaling(player, progress);
        if (scaling == null){
            return;
        }

        int duration = scaling.durationSteps * 200;
        shortenRandomEffects(player, target, MobEffectCategory.BENEFICIAL, scaling.effectCount, duration);
        List<MobEffectInstance> harmfulEffects = getActiveEffects(target, MobEffectCategory.HARMFUL);
        if (harmfulEffects.isEmpty()){
            grantRandomNegativeEffects(
                    player, target, scaling.effectCount, duration, scaling.amplifier);
        }else {
            changeRandomEffectDurations(
                    player, target, harmfulEffects, scaling.effectCount, duration, false);
        }
    }

    private static EffectScaling getEffectScaling(ServerPlayer player, double progress) {
        int guaranteed = (int) Math.floor(progress / 100.0);
        double remainder = progress % 100.0;
        if (guaranteed == 0){
            return roll(player, remainder) ? new EffectScaling(1, 1, 0) : null;
        }
        return new EffectScaling(
                guaranteed + (roll(player, remainder) ? 1 : 0),
                guaranteed + (roll(player, remainder) ? 1 : 0),
                guaranteed - 1 + (roll(player, remainder) ? 1 : 0));
    }

    private static boolean roll(ServerPlayer player, double percent) {
        return percent > 0.0 && player.getRandom().nextDouble() * 100.0 < percent;
    }

    private static void removeRandomNegativeEffects(ServerPlayer player, int count) {
        List<Holder<MobEffect>> negativeEffects = player.getActiveEffects().stream()
                                                .map(MobEffectInstance::getEffect)
                                                .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                                                .distinct()
                                                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < count && !negativeEffects.isEmpty(); i++) {
            Holder<MobEffect> effect = negativeEffects.remove(player.getRandom().nextInt(negativeEffects.size()));
            player.removeEffect(effect);
        }
    }

    private static void grantRandomPositiveEffects(
            ServerPlayer player,
            int count,
            int duration,
            int amplifier) {
        List<Holder<MobEffect>> candidates = new ArrayList<>(getPositiveEffectPool());
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            Holder<MobEffect> effect = candidates.remove(player.getRandom().nextInt(candidates.size()));
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private static void shortenRandomEffects(
            ServerPlayer player,
            LivingEntity target,
            MobEffectCategory category,
            int count,
            int duration) {
        changeRandomEffectDurations(
                player, target, getActiveEffects(target, category), count, duration, true);
    }

    private static void changeRandomEffectDurations(
            ServerPlayer player,
            LivingEntity target,
            List<MobEffectInstance> effects,
            int count,
            int duration,
            boolean shorten) {
        for (int i = 0; i < count && !effects.isEmpty(); i++) {
            MobEffectInstance effect = effects.remove(player.getRandom().nextInt(effects.size()));
            if (shorten){
                EntityHelper.shortenEffect(effect, target, duration);
            }else {
                EntityHelper.extendEffect(effect, target, duration);
            }
        }
    }

    private static List<MobEffectInstance> getActiveEffects(
            LivingEntity target,
            MobEffectCategory category) {
        return target.getActiveEffects().stream()
                     .filter(effect -> effect.getEffect().value().getCategory() == category)
                     .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void grantRandomNegativeEffects(
            ServerPlayer player,
            LivingEntity target,
            int count,
            int duration,
            int amplifier) {
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        BuiltInRegistries.POTION.forEach(potion -> potion.getEffects().stream()
                                                                    .map(MobEffectInstance::getEffect)
                                                                    .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                                                                    .filter(effect -> !candidates.contains(effect))
                                                                    .forEach(candidates::add));
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            Holder<MobEffect> effect = candidates.remove(player.getRandom().nextInt(candidates.size()));
            target.addEffect(new MobEffectInstance(effect, duration, amplifier), player);
        }
    }

    private static Set<Holder<MobEffect>> getPositiveEffectPool() {
        Set<Holder<MobEffect>> effects = new LinkedHashSet<>();
        if (MaledictConfig.AERIAL_POTION_EFFECTS_ONLY.get()){
            BuiltInRegistries.POTION.forEach(potion -> potion.getEffects().stream()
                                                                        .map(MobEffectInstance::getEffect)
                                                                        .filter(effect -> effect.value().getCategory() == MobEffectCategory.BENEFICIAL)
                                                                        .forEach(effects::add));
        }else {
            BuiltInRegistries.MOB_EFFECT.holders()
                                       .filter(effect -> effect.value().getCategory() == MobEffectCategory.BENEFICIAL)
                                       .forEach(effects::add);
        }
        return effects;
    }

    private record EffectScaling(int effectCount, int durationSteps, int amplifier) {
    }

    private IncursusBladeEffects() {
    }
}
