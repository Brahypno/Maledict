package org.brahypno.maledict.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.Maledict;

import javax.annotation.Nullable;

/**
 * 沉辉与反照共用的两种自有伤害类型，数据在 {@code data/maledict/damage_type/} 下。
 *
 * <p>两种伤害都走普通 {@code hurt}：护甲、附魔、抗性、吸收、灵魂护盾一个不少，<b>不是</b>
 * 绕过防御的特殊攻击，也不暗中改写血量。直接实体一律留空，只有造成者是那名攻击者 —— 这样它
 * 既不会被认成近战挥砍（沉辉的积蓄过滤据此排除自己），也能把掉落、经验与 Malum 的仇恨归属
 * 顺着伤害来源流回攻击者。
 */
public final class MaledictDamageTypes {
    /** 沉辉的定时单体释放与死亡范围释放共用同一类型。 */
    public static final ResourceKey<DamageType> LUMENFALL_RELEASE = create("lumenfall_release");

    /** 反照把充能带回目标身上的那一记，按魔法伤害注册。 */
    public static final ResourceKey<DamageType> REFULGENCE = create("refulgence");

    private static ResourceKey<DamageType> create(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name));
    }

    /**
     * 现查注册表而不是缓存 {@link Holder}：数据包重载会换掉 holder，缓存下来的那份会过期。
     *
     * @param causing 造成者；为空时退化成无主伤害，不为归属去加载区块或维度
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable LivingEntity causing) {
        Holder<DamageType> holder = level.registryAccess()
                                         .registryOrThrow(Registries.DAMAGE_TYPE)
                                         .getHolderOrThrow(type);
        return new DamageSource(holder, null, causing);
    }

    public static boolean is(DamageSource source, ResourceKey<DamageType> type) {
        return source.typeHolder().is(type);
    }

    /** 自家两种伤害：既不产生新的沉辉，也不给反照充能，避免一击自激成环。 */
    public static boolean isOwn(DamageSource source) {
        return is(source, LUMENFALL_RELEASE) || is(source, REFULGENCE);
    }

    private MaledictDamageTypes() {
    }
}
