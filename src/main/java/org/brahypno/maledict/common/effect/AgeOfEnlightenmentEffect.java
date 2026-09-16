package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 启蒙之年：佩戴启蒙之年的玩家击杀敌人后获得的药水效果。
 *
 * <p>归类为 {@link MobEffectCategory#BENEFICIAL}——它给的全是好处：佩戴者每次出手都必定暴击，
 * 且每级额外提供 1 点法杖暂存弹数。
 *
 * <h2>与 Malum 的 grim_certainty 的关系</h2>
 * 定位相同（都是「下一击必定暴击」的增益标记），实现方式刻意不同：
 * <ul>
 *   <li>Malum 那边 {@code GrimCertaintyEffect} 是个纯空标记——它连 {@code isDurationEffectTick}
 *       都返回 {@code false}，真正翻倍的是 {@code WeightOfWorldsItem} 自己；
 *       我们这边暴击挂在护符的全局事件上（见 {@code AgeOfEnlightenmentEvents#onCriticalHit}），
 *       因为这个效果是饰品给的，不属于任何一把武器。</li>
 *   <li>Malum 命中后会 {@code removeEffect}，也就是「只保一次」；这里<b>不消耗</b>，
 *       效果在持续时间内一直生效。这是这件饰品与监视者项链系设计的分野：启蒙之年靠击杀维持。</li>
 * </ul>
 *
 * <h2>为什么每级 +1 只要写一个 {@code 1.0}</h2>
 * 与 {@code AgeOfDarknessEffect} 同一个机制：原版 {@code addAttributeModifiers} 结算时会过
 * {@code getAttributeModifierValue(amplifier, modifier)}，实现是
 * {@code amount * (amplifier + 1)}。{@code RESERVE_STAFF_CHARGES} 默认 0、用
 * {@code ADDITION}，于是等级 I 就是 {@code +1}，等级 II 就是 {@code +2}。
 *
 * <h2>说明文字</h2>
 * 效果说明走的不是原版 tooltip，而是 JEED（Just Enough Effect Descriptions）读的
 * {@code effect.maledict.age_of_enlightenment.description}，与 Malum 的做法一致，见
 * {@code MaledictLanguage}。
 */
public final class AgeOfEnlightenmentEffect extends MobEffect {
    /** 金色。与托尔金的「一环」同款，能同时在深色与浅色 HUD 上读出来。 */
    public static final int COLOR = 0xFFC64B;

    /** 每级额外给的法杖暂存弹数。等级乘数由原版补上，这里只写单级的值。 */
    public static final double RESERVE_STAFF_CHARGES_PER_LEVEL = 1.0D;

    /**
     * 修饰符 UUID：由名字算死。
     *
     * <p>原版按 UUID 认修饰符（移除时比的是 id），固定值才能保证效果到期时摘干净。
     */
    private static final UUID RESERVE_STAFF_CHARGES_MODIFIER_ID = UUID.nameUUIDFromBytes(
            "maledict:age_of_enlightenment/reserve_staff_charges".getBytes(StandardCharsets.UTF_8));

    public AgeOfEnlightenmentEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        addAttributeModifier(AttributeRegistry.RESERVE_STAFF_CHARGES.get(),
                RESERVE_STAFF_CHARGES_MODIFIER_ID.toString(),
                RESERVE_STAFF_CHARGES_PER_LEVEL,
                AttributeModifier.Operation.ADDITION);
    }
}
