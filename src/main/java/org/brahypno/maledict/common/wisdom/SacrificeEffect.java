package org.brahypno.maledict.common.wisdom;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

/**
 * 「献祭」：标准九天九夜每分钟触发一次，每次固定 6 点，共 180 次。
 * 实际时长缩短或倒计时加速只提高频率；不统计伤害是否命中、减免或实际扣了多少血。
 *
 * <p>伤害走 {@code malum:voodoo}：它带 {@code bypasses_armor} / {@code bypasses_resistance} /
 * {@code bypasses_invulnerability} 三个标签，穿过护甲、抗性提升与创造模式免疫，也照样打得死人。无敌帧在
 * 结算时单独绕过（这个伤害类型没有 {@code bypasses_cooldown}），结算后保留原有保护。这里不带
 * 攻击者，所以死亡信息是 Malum 那句「灵魂被击碎了」，而不是把自己记成凶手。
 *
 * <p>计时进度随实体 NBT 保存；仪式施加的效果自然到期且玩家存活就发成就，见
 * {@code WisdomSacrifice}；中途死亡、被牛奶清掉或被人为移除都只是白挨一顿打。
 */
public final class SacrificeEffect extends MobEffect {
    /**
     * 效果底色：血红。类别是中性的，界面上按中性效果描边。
     */
    public static final int COLOR = 0x8C1F3F;

    private static final String PROGRESS_KEY = "maledict:sacrifice_schedule";

    public SacrificeEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 必须观察所有 tick：只检查 duration % 1200 会漏掉被加速跳过的节点。
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity wearer, int amplifier) {
        if (wearer.level().isClientSide){
            return;
        }

        MobEffectInstance effect = wearer.getEffect(this);
        if (effect == null) {
            return;
        }
        if (effect.isInfiniteDuration()) {
            if (wearer.tickCount % SacrificeSchedule.INTERVAL_TICKS == 0) {
                strike(wearer);
            }
            return;
        }
        if (!wearer.getPersistentData().contains(PROGRESS_KEY)) {
            start(wearer, effect.getDuration());
        }
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        triggerPulses(wearer, progress, SacrificeSchedule.targetPulses(
                progress.getInt("initialDuration"), effect.getDuration()));
    }

    /** 仪式重新施加时，触发进度与凭据一同重新开始；读取实际加上的效果时长。 */
    public static void start(LivingEntity wearer, int duration) {
        clear(wearer);
        CompoundTag progress = new CompoundTag();
        progress.putInt("initialDuration", duration);
        progress.putInt("pulses", 0);
        wearer.getPersistentData().put(PROGRESS_KEY, progress);
    }

    /** 自然到期时补上因倒计时跳减而尚未触发的次数，每次仍固定 6 点。 */
    public static void finish(LivingEntity wearer) {
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        if (progress.contains("initialDuration")) {
            triggerPulses(wearer, progress, SacrificeSchedule.PULSES);
        }
    }

    public static void clear(LivingEntity wearer) {
        wearer.getPersistentData().remove(PROGRESS_KEY);
        wearer.getPersistentData().remove("maledict:sacrifice_damage");
    }

    private static void triggerPulses(LivingEntity wearer, CompoundTag progress, int target) {
        while (progress.getInt("pulses") < target && wearer.isAlive()) {
            progress.putInt("pulses", progress.getInt("pulses") + 1);
            strike(wearer);
        }
    }

    private static void strike(LivingEntity wearer) {
        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO);
        // 加速后同 tick 内可能触发多次；voodoo 并不绕过受伤冷却。
        int invulnerableTime = wearer.invulnerableTime;
        wearer.invulnerableTime = 0;
        wearer.hurt(source, SacrificeSchedule.DAMAGE);
        wearer.invulnerableTime = Math.max(invulnerableTime, wearer.invulnerableTime);
    }
}
