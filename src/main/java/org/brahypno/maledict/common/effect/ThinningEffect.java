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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 汰余之令：灵魂木「汰余符文」给佩戴者的药水效果，也就是图腾「屠戮仪式」被刻进符板后的那一半。
 *
 * <p>名字不是「屠戮」：Malum 已经有一枚 {@code malum:rune_of_culling}（屠戮符文，走魔法伤害那条线），
 * 两个同名的符文摆在一起没法看。这里取「只汰去多余的部分」这个意思——它从来不是杀戮，
 * 是维持一个数目。
 *
 * <h2>与图腾的数值对照</h2>
 * <table>
 *   <tr><th></th><th>图腾（greater_wicked_rite 腐化面）</th><th>这枚符文</th></tr>
 *   <tr><td>范围</td><td>以基座为中心 9×9×9</td><td>以佩戴者为中心 5×5×5</td></tr>
 *   <tr><td>触发线</td><td>同族成年动物 ≥ 20 只</td><td>同种敌对生物 &gt; 8 只</td></tr>
 *   <tr><td>动作</td><td>一次把一族杀到只剩 19 只</td><td>每两秒给最外围的那只 1.5 颗心</td></tr>
 *   <tr><td>节奏</td><td>每 40 tick</td><td>每 40 tick</td></tr>
 * </table>
 *
 * <h2>为什么目标从牲畜换成敌对生物</h2>
 * 图腾图的是「过量的牲畜」，可那一套搬到随身符文上并不成立：20 只同种动物塞进 5×5×5（125 格）
 * 本来就少见，还得让佩戴者自己站进圈里，换来的只是每两秒少一只、掉两块肉。那是台牧场维护机，
 * 不是一枚符文该干的事。同一套「只汰多余」的逻辑对准敌群才立得住——被围住、刷怪笼、
 * 袭击，全是玩家真的会遇到的场面，而且越乱越有用。
 *
 * <h2>为什么不是秒杀</h2>
 * 秒杀等于一枚随身自动刷怪机，站在刷怪笼前就能看着怪自己倒下。这里改成伤害，而且比
 * {@link DecayEffect} 重一档：衰朽之息是半颗心且永不致死，这一记是 1.5 颗心、会打死人。
 * 一分钟三十点伤害、每次只打一只、还要同种敌人挤到八只以上才启动——它帮你削，但不替你打完。
 * 两根一起戴时分工清楚：一个磨，一个砍，都只打敌对之物。
 *
 * <h2>掉落与归属</h2>
 * 伤害类型是玩家源的 {@code malum:voodoo}：{@code DamageTypeHelper} 的三参重载把佩戴者填进「起因」
 * 那一格（直接来源留空），于是这一记算佩戴者的击杀——经验、精魂收获，连同手上武器的抢夺附魔，
 * 都按玩家击杀走。
 *
 * <p>数据包里 {@code malum:voodoo} 同时躺在 {@code bypasses_armor}、{@code bypasses_resistance}
 * 与 {@code bypasses_invulnerability} 三张表上：护甲、保护附魔、抗性提升都挡不住它，连无敌状态也照穿。
 * 所以这里显式补了一道 {@link net.minecraft.world.entity.Entity#isInvulnerable()}，
 * 免得符文去啃正处于无敌相位的东西。
 *
 * <p>代价是这枚符文真的能替你刷：同种敌人挤到八只以上时，一分钟最多三十点伤害，比玩家自己挥刀慢得多，
 * 但掉落和经验确实进你的口袋。想收回这层收益，就把 {@link #CROWD_LIMIT} 调高、或把伤害类型换回
 * {@code malum:voodoo_playerless}。
 *
 * <p>两个数都是旋钮：{@link #CROWD_LIMIT} 决定多挤才算过量，{@link #DAMAGE_PER_LEVEL} 决定一记多重。
 */
public final class ThinningEffect extends MobEffect {
    /** Malum 邪恶精魂的次色 {@code #4815FF}：与衰朽之息同一个家族，又分得开。 */
    public static final int COLOR = 0x4815FF;

    /** 每级每次的伤害。等级乘数由这里自己乘；衰朽之息是 1.0（半颗心），这里 3.0（一颗半）。 */
    public static final float DAMAGE_PER_LEVEL = 3.0F;

    /** 同种敌对生物挤到超过这个数才算「过量」。 */
    public static final int CROWD_LIMIT = 8;

    /** 以佩戴者所在方块为中心的水平与垂直半径，{@code 2} 即 5×5×5，图腾是 4（9×9×9）。 */
    public static final int RADIUS = 2;

    /** 出手间隔。与 {@code PulseRuneItem} 刷效果的节奏同为 40 tick，图腾也是这个数。 */
    public static final int INTERVAL_TICKS = 40;

    public ThinningEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    /**
     * 原版按 {@code duration % n == 0} 判定。符文每 40 tick 把时长刷回 200，
     * 200 / 160 / 120 / 80 / 40 都是 40 的倍数，于是每 40 tick 恰好出手一次。
     */
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
        Map<Class<? extends LivingEntity>, List<LivingEntity>> crowds = new LinkedHashMap<>();
        for (LivingEntity candidate : wearer.level().getEntitiesOfClass(LivingEntity.class, crowdArea(wearer))) {
            if (!isSurplus(wearer, candidate)) {
                continue;
            }
            crowds.computeIfAbsent(candidate.getClass(), kind -> new ArrayList<>()).add(candidate);
        }

        float damage = DAMAGE_PER_LEVEL * (amplifier + 1);
        for (List<LivingEntity> crowd : crowds.values()) {
            if (crowd.size() <= CROWD_LIMIT) {
                continue;
            }
            LivingEntity victim = outermost(wearer, crowd);
            ParticleEffectTypeRegistry.MAJOR_HEXING_SMOKE.createEntityEffect(victim,
                    new ColorEffectData(SpiritTypeRegistry.WICKED_SPIRIT.getPrimaryColor()));
            victim.hurt(source, damage);
            // 一次触发只打一只：这枚符文维持的是一条线，不是一场屠杀。
            return;
        }
    }

    private static AABB crowdArea(LivingEntity wearer) {
        return new AABB(wearer.blockPosition()).inflate(RADIUS);
    }

    private static boolean isSurplus(LivingEntity wearer, LivingEntity candidate) {
        if (candidate == wearer || candidate instanceof Player) {
            return false;
        }
        return candidate instanceof Enemy && candidate.isAlive() && !candidate.isInvulnerable();
    }

    /** 一堆里离佩戴者最远的那只先挨：要汰的是外围多出来的，不是贴脸这只。 */
    private static LivingEntity outermost(LivingEntity wearer, List<LivingEntity> crowd) {
        LivingEntity outermost = crowd.get(0);
        for (LivingEntity candidate : crowd) {
            if (candidate.distanceToSqr(wearer) > outermost.distanceToSqr(wearer)) {
                outermost = candidate;
            }
        }
        return outermost;
    }
}
