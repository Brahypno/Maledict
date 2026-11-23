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
import org.brahypno.maledict.common.entity.demiurge.DemiurgePillarEntity;
import org.brahypno.maledict.common.entity.demiurge.DemiurgeStatueEntity;
import org.brahypno.maledict.common.entity.demiurge.DemiurgeTearEntity;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.raven.RavenEntity;
import org.brahypno.maledict.common.entity.raven.RavenFeatherEntity;
import org.brahypno.maledict.common.item.weapon.remembrance.SpiritArrowEntity;
import org.brahypno.maledict.common.item.curio.hangman.HangmanTrapEntity;
import org.brahypno.maledict.common.item.curio.priestess.SoulEchoEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeLightOrbEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeScytheProjectileEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeSpiritBoltEntity;
import org.brahypno.maledict.common.entity.wither.OssuaryFangs;
import org.brahypno.maledict.common.entity.wither.WeepingWitherEntity;
import org.brahypno.maledict.common.entity.wither.WeepingWitherSkeletonEntity;
import org.brahypno.maledict.common.entity.wither.WeepingWitherSkull;

@EventBusSubscriber(modid = Maledict.MODID)
public final class MaledictEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Maledict.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DemiurgeStatueEntity>> DEMIURGE_STATUE =
            ENTITY_TYPES.register("demiurge_statue", () -> EntityType.Builder
                    .of(DemiurgeStatueEntity::new, MobCategory.MISC)
                    .sized(5.1F, 4.2F).clientTrackingRange(12).fireImmune()
                    .build("demiurge_statue"));

    public static final DeferredHolder<EntityType<?>, EntityType<DemiurgeTearEntity>> DEMIURGE_TEAR =
            ENTITY_TYPES.register("demiurge_tear", () -> EntityType.Builder
                    .of(DemiurgeTearEntity::new, MobCategory.MISC)
                    .sized(.2F, .6F).clientTrackingRange(10).updateInterval(1).noSave().noSummon().fireImmune()
                    .build("demiurge_tear"));

    public static final DeferredHolder<EntityType<?>, EntityType<DemiurgePillarEntity>> DEMIURGE_PILLAR =
            ENTITY_TYPES.register("demiurge_pillar", () -> EntityType.Builder
                    .of(DemiurgePillarEntity::new, MobCategory.MISC)
                    .sized(1.25F, 3.5F).clientTrackingRange(8).updateInterval(1).noSave().noSummon().fireImmune()
                    .build("demiurge_pillar"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoulEchoEntity>> SOUL_ECHO =
            ENTITY_TYPES.register("soul_echo", () -> EntityType.Builder
                    .of(SoulEchoEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)
                    .clientTrackingRange(8).updateInterval(20).noSave().noSummon().fireImmune()
                    .build("soul_echo"));

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

    /** Vanilla evoker-fang type dimensions, tracked the same way; it is not a Mob, so no attributes. */
    public static final DeferredHolder<EntityType<?>, EntityType<OssuaryFangs>> OSSUARY_FANGS =
            ENTITY_TYPES.register("ossuary_fangs", () -> EntityType.Builder
                    .<OssuaryFangs>of(OssuaryFangs::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("ossuary_fangs"));

    public static final DeferredHolder<EntityType<?>, EntityType<WeepingWitherEntity>> WEEPING_WITHER =
            ENTITY_TYPES.register("weeping_wither", () -> EntityType.Builder
                    .of(WeepingWitherEntity::new, MobCategory.MONSTER)
                    .sized(1.08F, 4.2F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build("weeping_wither"));

    public static final DeferredHolder<EntityType<?>, EntityType<WeepingWitherSkull>> WEEPING_WITHER_SKULL =
            ENTITY_TYPES.register("weeping_wither_skull", () -> EntityType.Builder
                    .<WeepingWitherSkull>of(WeepingWitherSkull::new, MobCategory.MISC)
                    .sized(0.3125F, 0.3125F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .fireImmune()
                    .build("weeping_wither_skull"));

    public static final DeferredHolder<EntityType<?>, EntityType<WeepingWitherSkeletonEntity>>
            WEEPING_WITHER_SKELETON =
            ENTITY_TYPES.register("weeping_wither_skeleton", () -> EntityType.Builder
                    .of(WeepingWitherSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("weeping_wither_skeleton"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SOUL_ECHO.get(), SoulEchoEntity.createLivingAttributes().build());
        event.put(FIRST_VICISSITUDE.get(), FirstVicissitudeBossEntity.createAttributes().build());
        event.put(DEMIURGE_STATUE.get(), DemiurgeStatueEntity.createAttributes().build());
        event.put(WEEPING_WITHER.get(), WeepingWitherEntity.createAttributes().build());
        event.put(WEEPING_WITHER_SKELETON.get(), WeepingWitherSkeletonEntity.createAttributes().build());
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
