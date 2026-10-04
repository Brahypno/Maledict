package org.brahypno.maledict.registry;

import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.corruption.CorruptionRuneEvents;
import org.brahypno.maledict.common.effect.AgeOfDarknessEffect;
import org.brahypno.maledict.common.effect.AgeOfEnlightenmentEffect;
import org.brahypno.maledict.common.effect.DecayEffect;
import org.brahypno.maledict.common.effect.SacrificeEffect;
import org.brahypno.maledict.common.effect.ThinningEffect;

public final class MaledictMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Maledict.MODID);

    /** 启蒙之年：击杀时给佩戴者，效果是近战伤害翻倍 + 每级 1 点法杖暂存弹数。 */
    public static final RegistryObject<MobEffect> AGE_OF_ENLIGHTENMENT =
            MOB_EFFECTS.register("age_of_enlightenment", AgeOfEnlightenmentEffect::new);

    /** 黑暗之年：击杀时给附近的一名敌人；每级 -20% 魔法抗性、灵魂护盾容量与稳固度。 */
    public static final RegistryObject<MobEffect> AGE_OF_DARKNESS =
            MOB_EFFECTS.register("age_of_darkness", AgeOfDarknessEffect::new);

    private static final int BLESSING_OF_LIFE_COLOR = 0xFF9ECF;

    /**
     * 生灵之祝：本身没有行为，自然回血的放大写在 {@code FoodDataMixin}/{@code BlessedRegeneration}。
     * 空的那对花括号省不掉：{@code MobEffect} 的构造函数是 {@code protected}，跨包只能借匿名子类。
     */
    public static final RegistryObject<MobEffect> BLESSING_OF_LIFE =
            MOB_EFFECTS.register("blessing_of_life", () -> new MobEffect(MobEffectCategory.BENEFICIAL, BLESSING_OF_LIFE_COLOR) {});

    /** 衰朽之息：等级 I 时每两秒把身边敌对之物磨掉半颗心，且永远不会因此送命。 */
    public static final RegistryObject<MobEffect> DECAY =
            MOB_EFFECTS.register("decay", DecayEffect::new);

    /**
     * 汰余之令：等级 I 时同种敌对生物挤到八只以上，最外围那只每两秒挨一记 1.5 颗心的重击。
     * 目前没有物品发放它。
     */
    public static final RegistryObject<MobEffect> THINNING =
            MOB_EFFECTS.register("thinning", ThinningEffect::new);

    /**
     * 献祭：牺牲仪式按在提尔锋持有者身上的中性效果，每分钟 6 点，九天九夜后自行到期时发「智慧的牺牲」。
     * 时间表在 {@code SacrificeSchedule}，发放写在 {@code WisdomSacrifice}。
     */
    public static final RegistryObject<MobEffect> SACRIFICE =
            MOB_EFFECTS.register("sacrifice", SacrificeEffect::new);

    private static final int RIPENING_COLOR = 0xEE2C88;

    /**
     * 熟成之赐：本身没有行为，经验加成写在 {@code RipeningBonus}/{@code RipeningEvents}；
     * 空花括号的原因同 {@link #BLESSING_OF_LIFE}。
     */
    public static final RegistryObject<MobEffect> RIPENING =
            MOB_EFFECTS.register("ripening", () -> new MobEffect(MobEffectCategory.BENEFICIAL, RIPENING_COLOR) {});

    /**
     * 堕落：堕落符文落在目标身上的窗口标记。本身没有行为，只在 {@code CorruptionRuneEvents} 里被问一句
     * {@code hasEffect}；唯一的数值作用是**按等级给目标加恶念转化**（每级 +0.02，最多 10 级）——
     * 等级由符文命中推高，量由原版按 {@code 基础值 × (amplifier + 1)} 缩放。
     *
     * <p>它没有自己的逻辑，所以不留单独的类：预设值在 {@code CorruptionRuneEvents}，这里用匿名子类注册。
     * 实例初始化块省不掉 —— {@code addAttributeModifier} 是 {@code protected}，跨包只有子类体里能调；
     * 属性在构造期取是安全的（见 {@code AGENTS.md}：注册事件里 attribute 先于 mob_effect）。
     */
    public static final RegistryObject<MobEffect> FALLEN = MOB_EFFECTS.register("fallen", () -> new MobEffect(
            MobEffectCategory.HARMFUL, CorruptionRuneEvents.FALLEN_COLOR) {
        {
            addAttributeModifier(AttributeRegistry.MALIGNANT_CONVERSION.get(),
                                 CorruptionRuneEvents.FALLEN_MODIFIER_ID.toString(),
                                 CorruptionRuneEvents.FALLEN_CONVERSION_PER_LEVEL,
                                 AttributeModifier.Operation.ADDITION);
        }
    });

    private MaledictMobEffects() {
    }
}
