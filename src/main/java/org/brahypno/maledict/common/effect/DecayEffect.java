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
 * 衰朽之息：符文木「衰朽符文」给佩戴者的药水效果，也就是图腾「衰朽仪式」被刻进符板后的那一半。
 *
 * <h2>与图腾的数值对照</h2>
 * <table>
 *   <tr><th></th><th>图腾（wicked_rite 自然面）</th><th>这枚符文</th></tr>
 *   <tr><td>伤害</td><td>2.0（一颗心）</td><td>1.0（半颗心）× 等级</td></tr>
 *   <tr><td>范围</td><td>以基座为中心 9×9×9</td><td>以佩戴者为中心 5×5×5</td></tr>
 *   <tr><td>目标</td><td>所有非玩家生物</td><td>只挑敌对之物</td></tr>
 *   <tr><td>节奏</td><td>每 40 tick</td><td>每 40 tick</td></tr>
 * </table>
 *
 * <p>图腾版会把宠物、村民和自己的牲畜一起磨血，因为它只排除 {@code Player}。符文是随身带着走的东西，
 * 撒得到处都是，「除玩家以外全打」这一条在这里不能照抄，所以只留 {@link Enemy}（僵尸、骷髅、史莱姆、
 * 疥猪兽这一类敌对生物），村民、牲畜、狼猫一概不碰。
 *
 * <h2>它永远杀不死东西</h2>
 * 只有当前生命高于 {@link #UNTOUCHABLE_HEALTH}（2.5，即一又四分之一颗心）才挨打，而单次伤害是 1.0，
 * 小于这道门槛，所以血量最低只会被压到 0.5（四分之一颗心）就停手。这是图腾那边就有的性质，
 * 符文版照留——它负责把东西磨到读秒，收尾仍旧要玩家自己动手，也因此不会有「挂机自动刷掉落」的问题。
 *
 * <h2>伤害类型：玩家源的巫毒</h2>
 * 用的是 {@code malum:voodoo}，并把佩戴者填在「起因」那一格上（{@code DamageTypeHelper} 的三参重载
 * 把直接来源留空、只填起因），所以这一下算佩戴者的账：经验、精魂收获，连同手上武器的抢夺附魔，
 * 都按玩家击杀结算。
 *
 * <p>数据包里 {@code malum:voodoo} 同时躺在 {@code bypasses_armor}、{@code bypasses_resistance}
 * 与 {@code bypasses_invulnerability} 三张表上：护甲、保护附魔、抗性提升都挡不住它，连无敌状态也照穿。
 * 所以这里显式补了一道 {@link net.minecraft.world.entity.Entity#isInvulnerable()}，
 * 免得符文去啃正处于无敌相位的东西。
 *
 * <p>等级 I 与 II 的伤害（1.0 / 2.0）都低于 2.5 的门槛，结构上不可能致死；III 级起伤害才追上门槛。
 * 符文本身只给等级 I，所以「磨到读秒、由人收尾」这条性质在游戏里成立。
 */
public final class DecayEffect extends MobEffect {
    /** Malum 邪恶精魂的主色 {@code #792CEC}，与图腾那边的巫毒黑烟同色。 */
    public static final int COLOR = 0x792CEC;

    /** 每级每次的伤害。等级乘数由这里自己乘，图腾是 2.0、符文砍半。 */
    public static final float DAMAGE_PER_LEVEL = 1.0F;

    /** 生命不高于这个值就不再挨打——非致死的下限，与图腾同值。 */
    public static final float UNTOUCHABLE_HEALTH = 2.5F;

    /** 以佩戴者所在方块为中心的水平与垂直半径，{@code 2} 即 5×5×5，图腾是 4（9×9×9）。 */
    public static final int RADIUS = 2;

    /** 出手间隔。与 {@code PulseRuneItem} 刷效果的节奏同为 40 tick，图腾也是这个数。 */
    public static final int INTERVAL_TICKS = 40;

    public DecayEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    /**
     * 原版按 {@code duration % n == 0} 判定。符文每 40 tick 把时长刷回 200，
     * 200 / 160 / 120 / 80 / 40 都是 40 的倍数，于是每 40 tick 恰好出手一次，不会漏也不会连发。
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
