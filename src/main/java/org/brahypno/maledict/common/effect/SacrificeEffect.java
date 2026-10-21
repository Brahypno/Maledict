package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.MalumDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.maledict.registry.MaledictMobEffects;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

/**
 * 「献祭」：九天九夜内出手 180 次，每次固定 6 点伤害。
 * 缩短或加速效果只提高出手频率，不提高单次伤害，也不检查实际受到的伤害。
 *
 * <p>伤害沿用 {@code malum:voodoo}，仅记录已出手的次数；减伤、免疫或事件取消
 * 都不影响仪式进度，也不追加伤害补偿。无敌帧在结算时单独绕过
 * （这个伤害类型没有 {@code bypasses_cooldown}），结算后保留原有保护。这里不带
 * 攻击者，所以死亡信息是 Malum 那句「灵魂被击碎了」，而不是把自己记成凶手。
 *
 * <p>出手进度随实体 NBT 保存；来自仪式的效果自然到期且玩家存活就发成就，见
 * {@code WisdomSacrifice}；中途死亡、被牛奶清掉或被人为移除都只是白挨一顿打。
 */
public final class SacrificeEffect extends MobEffect {
    /**
     * 效果底色：血红。类别是中性的，界面上按中性效果描边。
     */
    public static final int COLOR = 0x8C1F3F;

    private static final String PROGRESS_KEY = "maledict:sacrifice_damage";

    public SacrificeEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 必须观察所有 tick：只检查 duration % 1200 会漏掉被加速跳过的节点。
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity wearer, int amplifier) {
        if (wearer.level().isClientSide){
            return true;
        }

        MobEffectInstance effect = wearer.getEffect(MaledictMobEffects.SACRIFICE);
        if (effect == null) {
            return true;
        }
        if (!wearer.getPersistentData().contains(PROGRESS_KEY)) {
            start(wearer, effect.getDuration());
        }
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        int target;
        if (effect.isInfiniteDuration()) {
            long now = wearer.level().getGameTime();
            if (now < progress.getLong("nextPulse")) {
                return true;
            }
            progress.putLong("nextPulse", now + SacrificeSchedule.INTERVAL_TICKS);
            target = progress.getInt("pulses") + 1;
        } else {
            target = SacrificeSchedule.targetPulses(progress.getInt("initialDuration"), effect.getDuration());
        }
        settle(wearer, progress, target);
        return true;
    }

    /** 仪式重新施加时，出手进度与凭据一同重新开始；读取实际加上的效果时长。 */
    public static void start(LivingEntity wearer, int duration) {
        CompoundTag progress = new CompoundTag();
        progress.putInt("initialDuration", duration);
        progress.putLong("nextPulse", wearer.level().getGameTime());
        wearer.getPersistentData().put(PROGRESS_KEY, progress);
    }

    /** 自然到期时补上跳过的出手节点；完成只取决于存活，不设伤害数额门槛。 */
    public static boolean finish(LivingEntity wearer) {
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        if (progress.contains("initialDuration")) {
            settle(wearer, progress, SacrificeSchedule.PULSE_COUNT);
        }
        return wearer.isAlive();
    }

    public static void clear(LivingEntity wearer) {
        wearer.getPersistentData().remove(PROGRESS_KEY);
    }

    private static void settle(LivingEntity wearer, CompoundTag progress, int target) {
        for (int pulse = progress.getInt("pulses"); pulse < target && wearer.isAlive(); pulse++) {
            progress.putInt("pulses", pulse + 1);
            DamageSource source = DamageTypeHelper.create(wearer.level(), MalumDamageTypes.VOODOO);
            // 加速倒计时可能在一 tick 越过多个节点，每次仍固定 6 点。
            int invulnerableTime = wearer.invulnerableTime;
            wearer.invulnerableTime = 0;
            wearer.hurt(source, SacrificeSchedule.DAMAGE);
            wearer.invulnerableTime = Math.max(invulnerableTime, wearer.invulnerableTime);
        }
    }
}
