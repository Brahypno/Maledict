package org.brahypno.maledict.common.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.config.MaledictConfig;
import team.lodestar.lodestone.helpers.EntityHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncursusBladeEffects {
    @SubscribeEvent
    public static void applyInfernalLooting(LootingLevelEvent event) {
        if (event.getDamageSource() == null
                || !(event.getDamageSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!(weapon.getItem() instanceof IncursusBladeItem)) {
            return;
        }
        int bonus = Math.max(0, (int) Math.floor(IncursusBladeItem.getStat(
                weapon, IncursusBladeItem.INFERNAL_POWER)));
        event.setLootingLevel(event.getLootingLevel() + bonus);
    }

    public static void applyAerialEffect(ServerPlayer player, ItemStack weapon) {
        double progress = Math.max(0.0, IncursusBladeItem.getStat(
                weapon, IncursusBladeItem.AERIAL_PROGRESS));
        EffectScaling scaling = getEffectScaling(player, progress);
        if (scaling == null) {
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
        if (scaling == null) {
            return;
        }

        int duration = scaling.durationSteps * 200;
        shortenRandomEffects(player, target, MobEffectCategory.BENEFICIAL, scaling.effectCount, duration);
        List<MobEffectInstance> harmfulEffects = getActiveEffects(target, MobEffectCategory.HARMFUL);
        if (harmfulEffects.isEmpty()) {
            grantRandomNegativeEffects(
                    player, target, scaling.effectCount, duration, scaling.amplifier);
        } else {
            changeRandomEffectDurations(
                    player, target, harmfulEffects, scaling.effectCount, duration, false);
        }
    }

    private static EffectScaling getEffectScaling(ServerPlayer player, double progress) {
        int guaranteed = (int) Math.floor(progress / 100.0);
        double remainder = progress % 100.0;
        if (guaranteed == 0) {
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
        List<MobEffect> negativeEffects = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < count && !negativeEffects.isEmpty(); i++) {
            MobEffect effect = negativeEffects.remove(player.getRandom().nextInt(negativeEffects.size()));
            player.removeEffect(effect);
        }
    }

    private static void grantRandomPositiveEffects(ServerPlayer player,
                                                   int count,
                                                   int duration,
                                                   int amplifier) {
        List<MobEffect> candidates = new ArrayList<>(getPositiveEffectPool());
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            MobEffect effect = candidates.remove(player.getRandom().nextInt(candidates.size()));
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private static void shortenRandomEffects(ServerPlayer player,
                                             LivingEntity target,
                                             MobEffectCategory category,
                                             int count,
                                             int duration) {
        changeRandomEffectDurations(
                player, target, getActiveEffects(target, category), count, duration, true);
    }

    private static void changeRandomEffectDurations(ServerPlayer player,
                                                    LivingEntity target,
                                                    List<MobEffectInstance> effects,
                                                    int count,
                                                    int duration,
                                                    boolean shorten) {
        for (int i = 0; i < count && !effects.isEmpty(); i++) {
            MobEffectInstance effect = effects.remove(player.getRandom().nextInt(effects.size()));
            if (shorten) {
                EntityHelper.shortenEffect(effect, target, duration);
            } else {
                EntityHelper.extendEffect(effect, target, duration);
            }
        }
    }

    private static List<MobEffectInstance> getActiveEffects(LivingEntity target,
                                                            MobEffectCategory category) {
        return target.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().getCategory() == category)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void grantRandomNegativeEffects(ServerPlayer player,
                                                   LivingEntity target,
                                                   int count,
                                                   int duration,
                                                   int amplifier) {
        List<MobEffect> candidates = new ArrayList<>();
        ForgeRegistries.POTIONS.getValues().forEach(potion -> potion.getEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                .filter(effect -> !candidates.contains(effect))
                .forEach(candidates::add));
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            MobEffect effect = candidates.remove(player.getRandom().nextInt(candidates.size()));
            target.addEffect(new MobEffectInstance(effect, duration, amplifier), player);
        }
    }

    private static Set<MobEffect> getPositiveEffectPool() {
        Set<MobEffect> effects = new LinkedHashSet<>();
        if (MaledictConfig.AERIAL_POTION_EFFECTS_ONLY.get()) {
            ForgeRegistries.POTIONS.getValues().forEach(potion -> potion.getEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(effect -> effect.getCategory() == MobEffectCategory.BENEFICIAL)
                    .forEach(effects::add));
        } else {
            ForgeRegistries.MOB_EFFECTS.getValues().stream()
                    .filter(effect -> effect.getCategory() == MobEffectCategory.BENEFICIAL)
                    .forEach(effects::add);
        }
        return effects;
    }

    private record EffectScaling(int effectCount, int durationSteps, int amplifier) {
    }

    private IncursusBladeEffects() {
    }
}
