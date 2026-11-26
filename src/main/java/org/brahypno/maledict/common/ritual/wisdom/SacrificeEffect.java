package org.brahypno.maledict.common.ritual.wisdom;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import org.brahypno.maledict.registry.MaledictMobEffects;

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
 *
 * <p>渡鸦取眼是另一条收账路线，见 {@link #settleRemaining}。
 */
public final class SacrificeEffect extends MobEffect {
    /**
     * 效果底色：血红。类别是中性的，界面上按中性效果描边。
     */
    public static final int COLOR = 0x8C1F3F;

    private static final String PROGRESS_KEY = "maledict:sacrifice_schedule";

    /**
     * 已经由渡鸦一次结清：印记哪怕还挂在身上也不再逐分钟出手，{@link #start} 会把它抹掉重来。
     */
    private static final String SETTLED_KEY = "maledict:sacrifice_settled";

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
        if (wearer.getPersistentData().getBoolean(SETTLED_KEY)){
            return;
        }

        MobEffectInstance effect = wearer.getEffect(this);
        if (effect == null){
            return;
        }
        if (effect.isInfiniteDuration()){
            if (wearer.tickCount % SacrificeSchedule.INTERVAL_TICKS == 0){
                strike(wearer);
            }
            return;
        }
        if (!wearer.getPersistentData().contains(PROGRESS_KEY)){
            start(wearer, effect.getDuration());
        }
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        triggerPulses(wearer, progress, SacrificeSchedule.targetPulses(
                progress.getInt("initialDuration"), effect.getDuration()));
    }

    /**
     * 仪式重新施加时，触发进度与凭据一同重新开始；读取实际加上的效果时长。
     */
    public static void start(LivingEntity wearer, int duration) {
        clear(wearer);
        wearer.getPersistentData().remove(SETTLED_KEY);
        CompoundTag progress = new CompoundTag();
        progress.putInt("initialDuration", duration);
        progress.putInt("pulses", 0);
        wearer.getPersistentData().put(PROGRESS_KEY, progress);
    }

    /**
     * 自然到期时补上因倒计时跳减而尚未触发的次数，每次仍固定 6 点。
     */
    public static void finish(LivingEntity wearer) {
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        if (progress.contains("initialDuration")){
            triggerPulses(wearer, progress, SacrificeSchedule.PULSES);
        }
    }

    public static void clear(LivingEntity wearer) {
        wearer.getPersistentData().remove(PROGRESS_KEY);
        wearer.getPersistentData().remove("maledict:sacrifice_damage");
    }

    /**
     * 已经落下过几次。没有进度（凭空塞进来的效果，或根本没挨过第一 tick）算 0。
     */
    public static int pulses(LivingEntity wearer) {
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        return progress.contains("initialDuration") ? progress.getInt("pulses") : 0;
    }

    /**
     * 渡鸦取眼：把还没落下的次数合成一次结算，然后把印记摘掉。
     *
     * <p>伤害与逐分钟那一下同源同算法，只是数额换成 {@link SacrificeSchedule#remainingDamage}——
     * 穿护甲、穿抗性、穿创造免疫，照样打得死人。顺序由 {@code WisdomSacrifice#settleByRaven} 负责：
     * 成就先发，这一笔后结。
     *
     * <p>摘印记不是收尾排场：{@link #applyEffectTick} 发现没有进度时会拿当前剩余时长重新
     * {@link #start}，印记留着等于让献祭从头再走一遍。所以这里连效果一起摘，进度也清，并落下
     * {@link #SETTLED_KEY}——万一别的模组把 {@code MobEffectEvent.Remove} 取消了，印记还挂着也不会再出手。
     */
    public static void settleRemaining(LivingEntity wearer) {
        int remaining = SacrificeSchedule.remainingPulses(pulses(wearer));
        if (remaining > 0 && wearer.isAlive()){
            strike(wearer, remaining * SacrificeSchedule.DAMAGE);
        }
        wearer.getPersistentData().putBoolean(SETTLED_KEY, true);
        wearer.removeEffect(MaledictMobEffects.SACRIFICE.get());
        clear(wearer);
    }

    private static void triggerPulses(LivingEntity wearer, CompoundTag progress, int target) {
        while (progress.getInt("pulses") < target && wearer.isAlive()) {
            progress.putInt("pulses", progress.getInt("pulses") + 1);
            strike(wearer);
        }
    }

    private static void strike(LivingEntity wearer) {
        strike(wearer, SacrificeSchedule.DAMAGE);
    }

    private static void strike(LivingEntity wearer, float amount) {
        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO);
        // 加速后同 tick 内可能触发多次；voodoo 并不绕过受伤冷却。
        int invulnerableTime = wearer.invulnerableTime;
        wearer.invulnerableTime = 0;
        wearer.hurt(source, amount);
        wearer.invulnerableTime = Math.max(invulnerableTime, wearer.invulnerableTime);
    }
}
