package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

/**
 * 「献祭」：牺牲仪式按在提尔锋持有者身上的中性药水效果，每 1200 tick 从佩戴者身上抽走 6 点生命。
 *
 * <p>伤害走 {@code malum:voodoo}：它带 {@code bypasses_armor} / {@code bypasses_resistance} /
 * {@code bypasses_invulnerability} 三个标签，护甲、抗性提升与无敌帧都拦不住，也照样打得死人。这里不带
 * 攻击者，所以死亡信息是 Malum 那句「灵魂被击碎了」，而不是把自己记成凶手。
 *
 * <p>时长由仪式按 {@link SacrificeSchedule#DURATION_TICKS} 施加，效果自然到期才发成就，见
 * {@code WisdomSacrifice}；中途死亡、被牛奶清掉或被人为移除都只是白挨一顿打。
 */
public final class SacrificeEffect extends MobEffect {
    /**
     * 效果底色：血红。类别是中性的，界面上按中性效果描边。
     */
    public static final int COLOR = 0x8C1F3F;

    public SacrificeEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return SacrificeSchedule.isPulse(duration);
    }

    @Override
    public void applyEffectTick(LivingEntity wearer, int amplifier) {
        if (wearer.level().isClientSide){
            return;
        }

        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO);
        wearer.hurt(source, SacrificeSchedule.DAMAGE);
    }
}
