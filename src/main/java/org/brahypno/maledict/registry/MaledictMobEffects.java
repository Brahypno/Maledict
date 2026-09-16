package org.brahypno.maledict.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.AgeOfDarknessEffect;
import org.brahypno.maledict.common.effect.AgeOfEnlightenmentEffect;

public final class MaledictMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Maledict.MODID);

    /** 启蒙之年：击杀时给佩戴者，金色、正面分类。效果是出手必暴击 + 每级 1 点法杖暂存弹数。 */
    public static final RegistryObject<MobEffect> AGE_OF_ENLIGHTENMENT =
            MOB_EFFECTS.register("age_of_enlightenment", AgeOfEnlightenmentEffect::new);

    /** 黑暗之年：击杀时给附近的一名敌人，紫色、正面分类，但给的全是减益：每级 -20% 魔法抗性、灵魂护盾容量与稳固度。 */
    public static final RegistryObject<MobEffect> AGE_OF_DARKNESS =
            MOB_EFFECTS.register("age_of_darkness", AgeOfDarknessEffect::new);

    private MaledictMobEffects() {
    }
}
