package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

/**
 * 衰朽之息：给佩戴者的药水效果，每 40 tick 磨一次周围的敌对生物（只打 {@link Enemy}，不碰玩家与中立生物）。
 * 伤害是玩家源的 {@code malum:voodoo}；该类型穿甲/穿抗性/穿无敌，故这里自行判 {@code isInvulnerable}。
 */
public final class DecayEffect extends MobEffect {
    /** Malum 邪恶精魂的主色 {@code #792CEC}。 */
    public static final int COLOR = 0x792CEC;

    /** 每级每次的伤害，{@code 1.0} = 半颗心，等级乘数由调用处自己乘。 */
    public static final float DAMAGE_PER_LEVEL = 1.0F;

    /** 生命不高于此值就不再挨打；单次伤害 1.0 小于它，所以永远不会致死。 */
    public static final float UNTOUCHABLE_HEALTH = 2.5F;

    /** 以佩戴者所在方块为中心的水平与垂直半径，{@code 2} 即 5×5×5。 */
    public static final int RADIUS = 2;

    /** 出手间隔，40 tick。 */
    public static final int INTERVAL_TICKS = 40;

    public DecayEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    /** 原版按 {@code duration % n == 0} 判定；符文每 40 tick 把时长刷回 200，故恰好每 40 tick 出手一次。 */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity wearer, int amplifier) {
        if (wearer.level().isClientSide) {
            return;
        }

        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO, wearer);
        float damage = DAMAGE_PER_LEVEL * (amplifier + 1);
        for (LivingEntity victim : wearer.level().getEntitiesOfClass(LivingEntity.class, witheringArea(wearer))) {
            if (!canWither(wearer, victim)) {
                continue;
            }
            ParticleEffectTypeRegistry.HEXING_SMOKE.createEntityEffect(victim,
                    new ColorEffectData(SpiritTypeRegistry.WICKED_SPIRIT.getPrimaryColor()));
            victim.hurt(source, damage);
        }
    }

    private static AABB witheringArea(LivingEntity wearer) {
        return new AABB(wearer.blockPosition()).inflate(RADIUS);
    }

    private static boolean canWither(LivingEntity wearer, LivingEntity candidate) {
        if (candidate == wearer || candidate instanceof Player) {
            return false;
        }
        if (!(candidate instanceof Enemy) || !candidate.isAlive() || candidate.isInvulnerable()) {
            return false;
        }
        return candidate.getHealth() > UNTOUCHABLE_HEALTH;
    }
}
