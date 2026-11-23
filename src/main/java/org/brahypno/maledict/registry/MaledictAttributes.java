package org.brahypno.maledict.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.brahypno.changelib.DamageHelper.DamagePolicy;
import org.brahypno.changelib.DamageHelper.DamageProbeResult;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.DamageHelper.attribute.AttributeAmountReaders;
import org.brahypno.changelib.DamageHelper.attribute.AttributeDamageRegistry;
import org.brahypno.changelib.DamageHelper.attribute.AttributeDamageSources;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

@EventBusSubscriber(modid = Maledict.MODID)
public final class MaledictAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, Maledict.MODID);
    public static final DeferredHolder<Attribute, Attribute> TRANSCEND_OMEN = ATTRIBUTES.register("transcend_omen",
            () -> new RangedAttribute("attribute.name.maledict.transcend_omen", 0, 0, 1).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> DESCENT = ATTRIBUTES.register("descent",
            () -> new RangedAttribute("attribute.name.maledict.descent", 0, 0, Double.MAX_VALUE).setSyncable(true));

    /**
     * 碧水（冻结）通道。1.20.1 一直是属性：刀的主手修饰符把它写进实体属性，再由 ChangeLib 的
     * 属性派发器在每一次探针命中之后发一笔 {@code minecraft:freeze}（见下）。
     * 1.21 移植曾把它降级成「显式 freeze 探针」，于是投掷飞镰、飞升横扫、认主连带三处都得各自
     * 补一发，任何一处漏写就少一跳；现在恢复成属性，通道数量只剩一个来源。
     */
    public static final DeferredHolder<Attribute, Attribute> POWDER_SNOW_DAMAGE = ATTRIBUTES.register("powder_snow_damage",
            () -> new RangedAttribute("attribute.name.maledict.powder_snow_damage", 0, 0, 1024).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, TRANSCEND_OMEN);
            event.add(type, DESCENT);
            event.add(type, POWDER_SNOW_DAMAGE);
        });
    }

    /**
     * 碧水与奥术两条属性通道都交给 ChangeLib 的属性伤害派发器。
     *
     * <p>碧水：{@code minecraft:freeze}、{@code itemBacked()}（投掷中的刀靠物堆叠上的修饰符取值）、
     * 每次命中都派发（{@code allowAll}），默认 D0/尊重防御 —— 与 1.20.1 逐参数一致。
     *
     * <p>奥术：只接管 {@code lodestone_magic_takeover} 标签里的武器，且只由
     * {@code can_trigger_magic_damage} 的原始来源触发。配套的
     * {@code LodestoneAttributeEventHandlerMixin} 解除 Lodestone 自己那笔附加魔法伤害，避免双倍。
     */
    @SubscribeEvent
    public static void registerAttributeDamage(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AttributeDamageRegistry.register(
                    POWDER_SNOW_DAMAGE,
                    AttributeDamageSources.damageType(DamageTypes.FREEZE),
                    AttributeAmountReaders.itemBacked());
            AttributeDamageRegistry.register(
                    LodestoneAttributes.MAGIC_DAMAGE,
                    MaledictAttributes::magicDamageSource,
                    AttributeAmountReaders.itemBacked(),
                    MaledictAttributes::allowsMagicTakeover,
                    new DamagePolicy(DamageStage.D0_VANILLA_HURT,
                                     DamageProbeResult.Penetration.RESPECT, false, false));
        });
    }

    private static boolean allowsMagicTakeover(LivingEntity attacker, Entity target,
                                               DamageSource originalSource) {
        return originalSource.is(LodestoneDamageTypeTags.CAN_TRIGGER_MAGIC)
               && !originalSource.is(LodestoneDamageTypeTags.IS_MAGIC)
               && attacker.getMainHandItem().is(MaledictTags.LODESTONE_MAGIC_TAKEOVER);
    }

    /**
     * 与 Lodestone 原版那笔附加魔法伤害同形：致伤者是攻击者、直伤实体为空。
     * 直伤实体必须留空，启蒙时代等近战倍率读的正是 {@code source.getDirectEntity()}。
     */
    private static DamageSource magicDamageSource(LivingEntity attacker, Entity directEntity,
                                                  DamageSource originalSource) {
        Holder<DamageType> holder = attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.MAGIC);
        return new DamageSource(holder, null, attacker, originalSource.getSourcePosition());
    }

    private MaledictAttributes() {
    }
}
