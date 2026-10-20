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
 * 「献祭」：按实际初始时长和倒计时进度分摊 1080 点伤害，每服务器分钟结算一次。
 * 标准九天九夜仍是每分钟 6 点；缩短或加速的效果会在更短的时间里结清同一笔伤害。
 *
 * <p>伤害走 {@code malum:voodoo}：它带 {@code bypasses_armor} / {@code bypasses_resistance} /
 * {@code bypasses_invulnerability} 三个标签，穿过护甲、抗性提升与创造模式免疫，也照样打得死人。无敌帧在
 * 结算时单独绕过（这个伤害类型没有 {@code bypasses_cooldown}），结算后保留原有保护。这里不带
 * 攻击者，所以死亡信息是 Malum 那句「灵魂被击碎了」，而不是把自己记成凶手。
 *
 * <p>结算记录随实体 NBT 保存；效果自然到期且实际承受满额伤害才发成就，见
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
        if (!wearer.getPersistentData().contains(PROGRESS_KEY)) {
            start(wearer, effect.getDuration());
        }
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        long now = wearer.level().getGameTime();
        if (now < progress.getLong("nextPulse")) {
            return;
        }
        progress.putLong("nextPulse", now + SacrificeSchedule.INTERVAL_TICKS);
        double target = effect.isInfiniteDuration()
                ? progress.getDouble("paidDamage") + SacrificeSchedule.DAMAGE
                : SacrificeSchedule.targetDamage(progress.getInt("initialDuration"), effect.getDuration());
        settle(wearer, progress, target);
    }

    /** 仪式重新施加时，伤害预算与凭据一同重新开始；读取实际加上的效果时长。 */
    public static void start(LivingEntity wearer, int duration) {
        CompoundTag progress = new CompoundTag();
        progress.putInt("initialDuration", duration);
        progress.putLong("nextPulse", wearer.level().getGameTime());
        wearer.getPersistentData().put(PROGRESS_KEY, progress);
    }

    /** 自然到期时补齐尾款；被取消、免疫或减掉的伤害不能充当已经承受的代价。 */
    public static boolean finish(LivingEntity wearer) {
        CompoundTag progress = wearer.getPersistentData().getCompound(PROGRESS_KEY);
        if (!progress.contains("initialDuration")) {
            return false;
        }
        settle(wearer, progress, SacrificeSchedule.TOTAL_DAMAGE);
        return wearer.isAlive() && SacrificeSchedule.isComplete(progress.getDouble("paidDamage"));
    }

    public static void clear(LivingEntity wearer) {
        wearer.getPersistentData().remove(PROGRESS_KEY);
    }

    private static void settle(LivingEntity wearer, CompoundTag progress, double target) {
        double paid = progress.getDouble("paidDamage");
        float due = (float) Math.max(0.0D, target - paid);
        if (due <= 0.0F || !wearer.isAlive()) {
            return;
        }
        float health = wearer.getHealth();
        float absorption = wearer.getAbsorptionAmount();
        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO);
        // 尾款可能紧接着上一笔；voodoo 的 bypasses_invulnerability 并不绕过受伤冷却。
        int invulnerableTime = wearer.invulnerableTime;
        wearer.invulnerableTime = 0;
        wearer.hurt(source, due);
        wearer.invulnerableTime = Math.max(invulnerableTime, wearer.invulnerableTime);
        double taken = Math.max(0.0D, health - wearer.getHealth())
                + Math.max(0.0D, absorption - wearer.getAbsorptionAmount());
        progress.putDouble("paidDamage", paid + Math.min(due, taken));
    }
}
