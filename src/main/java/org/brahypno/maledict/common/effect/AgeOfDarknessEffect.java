package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 黑暗时代：佩戴启蒙之年击杀敌人时，附近一名敌人获得的药水效果。
 *
 * <p><b>它是一个「正面的负面效果」</b>：{@link MobEffectCategory#BENEFICIAL} 让 HUD 走正面效果的边框，
 * 但它给的三个属性修饰符全是减益——每级削减 20% 魔法抗性、灵魂护盾容量、灵魂护盾稳固度。
 * 这个错位是刻意的，和启蒙之年成对：黑暗时代读起来像恩赐，实际是削弱。
 *
 * <p>紫色取 Malum 邪恶精魂的紫（{@code 121, 44, 236}，即 {@code #792CEC}），
 * 和 Malum 的贴图、粒子摆在一起不会打架。
 *
 * <h2>为什么每级 20% 只要写一个 {@code -0.2}</h2>
 * 原版 {@link MobEffect#addAttributeModifier} 收的是固定数值，看上去没法按等级缩放，但
 * {@link MobEffect#addAttributeModifiers} 结算时会过一道
 * {@code getAttributeModifierValue(amplifier, modifier)}，它的实现是
 * {@code modifier.getAmount() * (amplifier + 1)}——等级乘数由原版自己乘上去。
 * 所以这里写 {@code -0.2}，等级 I 就是 {@code -20%}，等级 II 就是 {@code -40%}，
 * 不需要 {@code applyEffectTick} 每 tick 手动增删修饰符。
 *
 * <p>Malum 自己的暴食也是这么写的（固定 {@code +0.2} 魔法熟练度），这条路子是一致的。
 *
 * <h2>为什么用 {@code MULTIPLY_TOTAL}</h2>
 * 三个属性都是「基数 × 百分比」型的：魔法抗性默认 1（Lodestone 的
 * {@code RangedAttribute(desc, 1, 0, 2048)}），灵魂护盾稳固度默认 1，容量默认 0。
 * {@code MULTIPLY_TOTAL} 的效果是 {@code value *= (1 + amount)}，也就是
 * {@code 1 * (1 - 0.2n)}：抗性与稳固度按比例掉，容量不管你堆到多少都同步掉两成。
 * 用 {@code ADDITION} 就变成「减 0.2 点」，对容量那种动辄几十点的属性毫无意义。
 *
 * <p>三个属性都声明了最小 0，{@link net.minecraft.world.entity.ai.attributes.AttributeInstance}
 * 算出结果后会过 {@code sanitizeValue} 夹一次，所以等级叠到把系数压穿 0 时是被夹到 0
 * （护盾一碰就碎），不会变成负数反过来加护盾。
 *
 * <h2>说明文字</h2>
 * 效果说明走的不是原版 tooltip，而是 JEED（Just Enough Effect Descriptions）读的
 * {@code effect.maledict.age_of_darkness.description}，与 Malum 的做法一致，见
 * {@code MaledictLanguage}。
 */
public final class AgeOfDarknessEffect extends MobEffect {
    /** Malum 邪恶精魂紫 {@code #792CEC}。 */
    public static final int COLOR = 0x792CEC;

    /** 每级削减的比例，{@code 0.2} 即 20%。等级乘数由原版补上，这里只写单级的值。 */
    public static final double REDUCTION_PER_LEVEL = 0.2D;

    /**
     * 三个修饰符各用一个由名字算死的 UUID。
     *
     * <p>原版的 {@code addAttributeModifiers} 与 {@code removeAttributeModifiers} 都是按 UUID
     * 认修饰符的（移除时 {@code AttributeInstance.removeModifier} 比的是 id），换个 UUID 就摘不干净。
     * 名字里带属性名是为了三者在调试时能一眼分辨，也让效果移除后能稳定复原。
     */
    private static final UUID MAGIC_RESISTANCE_MODIFIER_ID = modifierId("magic_resistance");
    private static final UUID SOUL_WARD_CAP_MODIFIER_ID = modifierId("soul_ward_cap");
    private static final UUID SOUL_WARD_INTEGRITY_MODIFIER_ID = modifierId("soul_ward_integrity");

    public AgeOfDarknessEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        strip(LodestoneAttributeRegistry.MAGIC_RESISTANCE.get(), MAGIC_RESISTANCE_MODIFIER_ID);
        strip(AttributeRegistry.SOUL_WARD_CAP.get(), SOUL_WARD_CAP_MODIFIER_ID);
        strip(AttributeRegistry.SOUL_WARD_INTEGRITY.get(), SOUL_WARD_INTEGRITY_MODIFIER_ID);
    }

    private void strip(Attribute attribute, UUID modifierId) {
        addAttributeModifier(attribute, modifierId.toString(), -REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static UUID modifierId(String attribute) {
        return UUID.nameUUIDFromBytes(
                ("maledict:age_of_darkness/" + attribute).getBytes(StandardCharsets.UTF_8));
    }
}
