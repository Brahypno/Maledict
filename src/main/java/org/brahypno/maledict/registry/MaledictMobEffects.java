package org.brahypno.maledict.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.AgeOfDarknessEffect;
import org.brahypno.maledict.common.effect.AgeOfEnlightenmentEffect;
import org.brahypno.maledict.common.effect.DecayEffect;
import org.brahypno.maledict.common.effect.ThinningEffect;

public final class MaledictMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Maledict.MODID);

    /**
     * 启蒙之年：击杀时给佩戴者，金色、正面分类。效果是出手必暴击 + 每级 1 点法杖暂存弹数。
     */
    public static final RegistryObject<MobEffect> AGE_OF_ENLIGHTENMENT =
            MOB_EFFECTS.register("age_of_enlightenment", AgeOfEnlightenmentEffect::new);

    /**
     * 黑暗之年：击杀时给附近的一名敌人，紫色、正面分类，但给的全是减益：每级 -20% 魔法抗性、灵魂护盾容量与稳固度。
     */
    public static final RegistryObject<MobEffect> AGE_OF_DARKNESS =
            MOB_EFFECTS.register("age_of_darkness", AgeOfDarknessEffect::new);

    /**
     * 生灵之祝的粉色。与「生灵」相称，在深色与浅色 HUD 上都读得出来。
     */
    private static final int BLESSING_OF_LIFE_COLOR = 0xFF9ECF;

    /**
     * 生灵之祝：粉色、正面分类，靠饱食度与饥饿值的自然回血生效，每级多回一份（等级 I 即翻倍）。
     *
     * <p>它自己没有行为——原版那两条自然回血发生在 {@code FoodData#tick} 里，没有任何事件能认出
     * 「这一次 heal 是它发的」，所以放大写在 {@code FoodDataMixin} 与 {@code BlessedRegeneration}，
     * 效果这边只登记名字与颜色，不必为它开一个空的具名子类。
     *
     * <p>那对空花括号省不掉：{@code MobEffect} 的构造函数是 {@code protected}，原版
     * {@code MobEffects} 能直接 {@code new} 是因为它同包，我们跨包就只能借一层匿名子类。
     *
     * <p>效果说明走 JEED 读的 {@code effect.maledict.blessing_of_life.description}，见 {@code MaledictLanguage}。
     */
    public static final RegistryObject<MobEffect> BLESSING_OF_LIFE =
            MOB_EFFECTS.register("blessing_of_life", () -> new MobEffect(MobEffectCategory.BENEFICIAL, BLESSING_OF_LIFE_COLOR) {});

    /**
     * 衰朽之息：符文木「衰朽符文」维持的效果。等级 I 时，每两秒把身边敌对之物磨掉半颗心，
     * 且永远不会因此送命——图腾「衰朽仪式」刻上符板后的那一半。
     */
    public static final RegistryObject<MobEffect> DECAY =
            MOB_EFFECTS.register("decay", DecayEffect::new);

    /**
     * 汰余之令：灵魂木「汰余符文」维持的效果。等级 I 时，同种敌对生物挤到八只以上，
     * 最外围的那只每两秒挨一记 1.5 颗心的重击——图腾「屠戮仪式」刻上符板后的那一半。
     */
    public static final RegistryObject<MobEffect> THINNING =
            MOB_EFFECTS.register("thinning", ThinningEffect::new);

    /** 熟成之赐的粉色：Malum 神圣精魂的主色 {@code #EE2C88}。 */
    private static final int RIPENING_COLOR = 0xEE2C88;

    /**
     * 熟成之赐：神圣线的灵魂木符文「熟成符文」维持的效果，把佩戴者获得的经验抬高四分之一。
     *
     * <p>它自己没有行为——原版的经验发放发生在 {@code Player#giveExperiencePoints} 里，效果碰不到，
     * 所以加成写在 {@code RipeningBonus}、挂钩写在 {@code RipeningEvents}，效果这边只登记名字与颜色。
     * 与 {@link #BLESSING_OF_LIFE} 同理，空的那对花括号省不掉：{@code MobEffect} 的构造函数是
     * {@code protected}，我们跨包就只能借一层匿名子类。
     */
    public static final RegistryObject<MobEffect> RIPENING =
            MOB_EFFECTS.register("ripening", () -> new MobEffect(MobEffectCategory.BENEFICIAL, RIPENING_COLOR) {});

    private MaledictMobEffects() {
    }
}
