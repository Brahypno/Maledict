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
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.changelib.DamageHelper.DamagePolicy;
import org.brahypno.changelib.DamageHelper.DamageProbeResult;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.DamageHelper.attribute.AttributeAmountReaders;
import org.brahypno.changelib.DamageHelper.attribute.AttributeDamageRegistry;
import org.brahypno.changelib.DamageHelper.attribute.AttributeDamageSourceFactory;
import org.brahypno.changelib.DamageHelper.attribute.AttributeDamageSources;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Maledict.MODID);
    public static final RegistryObject<Attribute> TRANSCEND_OMEN = ATTRIBUTES.register("transcend_omen",
            () -> new RangedAttribute("attribute.name.maledict.transcend_omen", 0, 0, 1).setSyncable(true));
    public static final RegistryObject<Attribute> DESCENT = ATTRIBUTES.register("descent",
            () -> new RangedAttribute("attribute.name.maledict.descent", 0, 0, Double.MAX_VALUE).setSyncable(true));

    /**
     * 神侵恶刃的冻结通道，同时也是 ChangeLib 属性伤害白名单中的一项。
     *
     * <p>值来自刀 NBT 的成长，主手属性修饰符把它写进实体属性；真正的结算由
     * {@link AttributeDamageRegistry} 在原始命中之后按 {@link DamageTypes#FREEZE} 发一笔独立的
     * {@code DamageRequestEvent}。沿用原版 {@code minecraft:freeze} 是为了保住伤害消息
     * {@code freeze}（无常的适应轮换、现有标签和测试都认这个 id）。
     */
    public static final RegistryObject<Attribute> POWDER_SNOW_DAMAGE = ATTRIBUTES.register("powder_snow_damage",
            () -> new RangedAttribute("attribute.name.maledict.powder_snow_damage", 0, 0, 1024).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, TRANSCEND_OMEN.get());
            event.add(type, DESCENT.get());
            event.add(type, POWDER_SNOW_DAMAGE.get());
        });
    }

    /**
     * 属性注册完成后把冻结属性和 Lodestone 的 {@code MAGIC_DAMAGE} 交给 ChangeLib。
     *
     * <p>冻结属性默认从 D0 / 尊重防御起步；神侵恶刃自己的档位由
     * {@code IncursusBladeEffects} 在 {@code DamageRequestEvent} 里覆盖，无忧符文在
     * {@code BlissRuneEvents} 里再抬一级。
     *
     * <p>魔法属性复用 Lodestone 的 {@code MAGIC_DAMAGE} 和 {@code minecraft:magic} 伤害类型，
     * 但只接管 {@link MaledictTags#LODESTONE_MAGIC_TAKEOVER} 标签里的武器，并且只从
     * {@code forge:can_trigger_magic_damage} 的原始来源触发。对应的
     * {@code LodestoneAttributeEventHandlerMixin} 在同一个标签/来源条件下取消 Lodestone 自己的
     * 附加魔法伤害，避免双份。
     */
    @SubscribeEvent
    public static void registerAttributeDamage(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AttributeDamageRegistry.register(
                    POWDER_SNOW_DAMAGE.get(),
                    AttributeDamageSources.damageType(DamageTypes.FREEZE),
                    AttributeAmountReaders.itemBacked());
            AttributeDamageRegistry.register(
                    LodestoneAttributeRegistry.MAGIC_DAMAGE.get(),
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
     * Matches Lodestone's old extra magic hit: causing entity is the attacker, direct entity is
     * null. Keeping the direct entity null matters: Age of Enlightenment and similar melee
     * multipliers check {@code source.getDirectEntity()}.
     */
    private static DamageSource magicDamageSource(LivingEntity attacker, Entity directEntity,
                                                  DamageSource originalSource) {
        Holder<DamageType> holder = attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MAGIC);
        return new DamageSource(holder, null, attacker, originalSource.getSourcePosition());
    }
}
