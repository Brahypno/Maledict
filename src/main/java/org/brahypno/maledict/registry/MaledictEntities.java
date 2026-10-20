package org.brahypno.maledict.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.raven.RavenEntity;
import org.brahypno.maledict.common.entity.raven.RavenFeatherEntity;
import org.brahypno.maledict.common.entity.projectile.SpiritArrowEntity;
import org.brahypno.maledict.common.entity.projectile.HangmanTrapEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeLightOrbEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeScytheProjectileEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeSpiritBoltEntity;

@EventBusSubscriber(modid = Maledict.MODID)
public final class MaledictEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Maledict.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HangmanTrapEntity>> HANGMAN_TRAP =
            ENTITY_TYPES.register("hangman_trap", () -> EntityType.Builder
                    .<HangmanTrapEntity>of(HangmanTrapEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(4).updateInterval(20)
                    .build("hangman_trap"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritArrowEntity>> SPIRIT_ARROW =
            ENTITY_TYPES.register("spirit_arrow", () -> EntityType.Builder
                    .<SpiritArrowEntity>of(SpiritArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("spirit_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<FirstVicissitudeBossEntity>> FIRST_VICISSITUDE =
            ENTITY_TYPES.register("first_vicissitude", () -> EntityType.Builder
                    .of(FirstVicissitudeBossEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 4.375F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("first_vicissitude"));

    public static final DeferredHolder<EntityType<?>, EntityType<RavenEntity>> RAVEN =
            ENTITY_TYPES.register("raven", () -> EntityType.Builder
                    .of(RavenEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("raven"));

    public static final DeferredHolder<EntityType<?>, EntityType<RavenFeatherEntity>> RAVEN_FEATHER =
            ENTITY_TYPES.register("raven_feather", () -> EntityType.Builder
                    .<RavenFeatherEntity>of(RavenFeatherEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("raven_feather"));

    public static final DeferredHolder<EntityType<?>, EntityType<VicissitudeSpiritBoltEntity>> VICISSITUDE_SPIRIT_BOLT =
            ENTITY_TYPES.register("vicissitude_spirit_bolt", () -> EntityType.Builder
                    .<VicissitudeSpiritBoltEntity>of(VicissitudeSpiritBoltEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build("vicissitude_spirit_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<VicissitudeScytheProjectileEntity>> VICISSITUDE_SCYTHE =
            ENTITY_TYPES.register("vicissitude_scythe", () -> EntityType.Builder
                    .<VicissitudeScytheProjectileEntity>of(VicissitudeScytheProjectileEntity::new,
                            MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build("vicissitude_scythe"));

    public static final DeferredHolder<EntityType<?>, EntityType<VicissitudeLightOrbEntity>> VICISSITUDE_LIGHT_ORB =
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
        event.put(RAVEN.get(), RavenEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(RAVEN.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RavenEntity::checkRavenSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private MaledictEntities() {
    }
}
