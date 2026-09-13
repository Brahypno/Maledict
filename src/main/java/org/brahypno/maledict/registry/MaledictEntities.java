package org.brahypno.maledict.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.SpiritArrowEntity;
import org.brahypno.maledict.common.entity.VicissitudeLightOrbEntity;
import org.brahypno.maledict.common.entity.VicissitudeScytheProjectileEntity;
import org.brahypno.maledict.common.entity.VicissitudeSpiritBoltEntity;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Maledict.MODID);

    public static final RegistryObject<EntityType<SpiritArrowEntity>> SPIRIT_ARROW =
            ENTITY_TYPES.register("spirit_arrow", () -> EntityType.Builder
                    .<SpiritArrowEntity>of(SpiritArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("spirit_arrow"));

    public static final RegistryObject<EntityType<FirstVicissitudeBossEntity>> FIRST_VICISSITUDE =
            ENTITY_TYPES.register("first_vicissitude", () -> EntityType.Builder
                    .of(FirstVicissitudeBossEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 4.375F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("first_vicissitude"));

    public static final RegistryObject<EntityType<VicissitudeSpiritBoltEntity>> VICISSITUDE_SPIRIT_BOLT =
            ENTITY_TYPES.register("vicissitude_spirit_bolt", () -> EntityType.Builder
                    .<VicissitudeSpiritBoltEntity>of(VicissitudeSpiritBoltEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build("vicissitude_spirit_bolt"));

    public static final RegistryObject<EntityType<VicissitudeScytheProjectileEntity>> VICISSITUDE_SCYTHE =
            ENTITY_TYPES.register("vicissitude_scythe", () -> EntityType.Builder
                    .<VicissitudeScytheProjectileEntity>of(VicissitudeScytheProjectileEntity::new,
                            MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build("vicissitude_scythe"));

    public static final RegistryObject<EntityType<VicissitudeLightOrbEntity>> VICISSITUDE_LIGHT_ORB =
            ENTITY_TYPES.register("vicissitude_light_orb", () -> EntityType.Builder
                    .<VicissitudeLightOrbEntity>of(VicissitudeLightOrbEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build("vicissitude_light_orb"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FIRST_VICISSITUDE.get(), FirstVicissitudeBossEntity.createAttributes().build());
    }

    private MaledictEntities() {
    }
}
