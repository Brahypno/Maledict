package org.brahypno.maledict.client;

import com.sammy.malum.common.item.IVoidItem;
import com.sammy.malum.visual_effects.ScreenParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.combat.vitals.DelayedVitalsOverlay;
import org.brahypno.maledict.client.entity.raven.RavenFeatherRenderer;
import org.brahypno.maledict.client.entity.raven.RavenModel;
import org.brahypno.maledict.client.entity.raven.RavenRenderer;
import org.brahypno.maledict.client.entity.raven.RavenShoulderLayer;
import org.brahypno.maledict.client.entity.vicissitude.model.FirstVicissitudeBossModel;
import org.brahypno.maledict.client.entity.vicissitude.render.FirstVicissitudeBossRenderer;
import org.brahypno.maledict.client.entity.vicissitude.render.VicissitudeLightOrbRenderer;
import org.brahypno.maledict.client.entity.vicissitude.render.VicissitudeScytheRenderer;
import org.brahypno.maledict.client.entity.vicissitude.render.VicissitudeSpiritBoltRenderer;
import org.brahypno.maledict.client.entity.vicissitude.vfx.VicissitudeSummoningPostProcessor;
import org.brahypno.maledict.client.infrared.InfraredPostProcessor;
import org.brahypno.maledict.client.item.curio.enlightenment.AgeOfEnlightenmentCurioRenderer;
import org.brahypno.maledict.client.item.curio.enlightenment.AgeOfEnlightenmentModel;
import org.brahypno.maledict.client.item.curio.hangman.HangmanTrapRenderer;
import org.brahypno.maledict.client.item.curio.priestess.SoulEchoRenderer;
import org.brahypno.maledict.client.item.weapon.remembrance.SpiritArrowRenderer;
import org.brahypno.maledict.client.item.weapon.scythe.DiplopiaScytheBoomerangRenderer;
import org.brahypno.maledict.client.realm.BlightedRealmEffects;
import org.brahypno.maledict.common.combat.lumenfall.LumenfallRadianceEffect;
import org.brahypno.maledict.common.item.weapon.remembrance.SpiritArrowItem;
import org.brahypno.maledict.common.realm.BlightedRealm;
import org.brahypno.maledict.common.ritual.invocation.InvocationDefinition;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.systems.particle.screen.ScreenParticleHolder;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 客户端 MOD 总线注册入口：实体与图层、饰品、HUD、物品颜色与粒子、后处理器。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class MaledictClientSetup {

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(BlightedRealm.ID, new BlightedRealmEffects());
    }

    private static AgeOfEnlightenmentModel ageOfEnlightenmentModel;
    private static final IVoidItem VOID_PARTICLES = new IVoidItem() {
    };

    private static final ParticleEmitterHandler.ItemParticleSupplier ESOTERICA_PARTICLES =
            new ParticleEmitterHandler.ItemParticleSupplier() {
                @Override
                public void spawnLateParticles(
                        ScreenParticleHolder target, Level level, float partialTick,
                        ItemStack stack, float x, float y) {
                    ScreenParticleEffects.spawnEncyclopediaEsotericaScreenParticles(target, level, partialTick);
                }
            };

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MaledictEntities.SOUL_ECHO.get(), SoulEchoRenderer::new);
        event.registerEntityRenderer(MaledictEntities.HANGMAN_TRAP.get(), HangmanTrapRenderer::new);
        event.registerEntityRenderer(MaledictEntities.SPIRIT_ARROW.get(), SpiritArrowRenderer::new);
        event.registerEntityRenderer(MaledictEntities.RAVEN.get(), RavenRenderer::new);
        event.registerEntityRenderer(MaledictEntities.RAVEN_FEATHER.get(), RavenFeatherRenderer::new);
        event.registerEntityRenderer(MaledictEntities.FIRST_VICISSITUDE.get(), FirstVicissitudeBossRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), VicissitudeLightOrbRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SPIRIT_BOLT.get(), VicissitudeSpiritBoltRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SCYTHE.get(), VicissitudeScytheRenderer::new);
        event.registerEntityRenderer(MaledictEntities.DEMIURGE_STATUE.get(),
                                     org.brahypno.maledict.client.entity.demiurge.DemiurgeStatueRenderer::new);
        event.registerEntityRenderer(MaledictEntities.DEMIURGE_PILLAR.get(),
                                     org.brahypno.maledict.client.entity.demiurge.DemiurgePillarRenderer::new);
        event.registerEntityRenderer(MaledictEntities.DEMIURGE_TEAR.get(),
                                     org.brahypno.maledict.client.entity.demiurge.DemiurgeTearRenderer::new);
        event.registerEntityRenderer(MaledictEntities.WEEPING_WITHER.get(),
                                     org.brahypno.maledict.client.entity.weepingWither.WeepingWitherRenderer::new);
        event.registerEntityRenderer(MaledictEntities.WEEPING_WITHER_SKULL.get(),
                                     org.brahypno.maledict.client.entity.weepingWither.WeepingWitherSkullRenderer::new);
        event.registerEntityRenderer(MaledictEntities.WEEPING_WITHER_SKELETON.get(),
                                     org.brahypno.maledict.client.entity.weepingWither.WeepingWitherSkeletonRenderer::new);
        event.registerEntityRenderer(MaledictEntities.OSSUARY_FANGS.get(),
                                     org.brahypno.maledict.client.entity.weepingWither.OssuaryFangsRenderer::new);
        // 复视的幻影沿用 Malum 的 scythe_boomerang 实体类型（1.8.2 的回旋镰刀构造器把类型写死），
        // 靠物品堆叠上的标记区分；未标记的镰刀由渲染器原样转调 Malum 的实现。
        event.registerEntityRenderer(com.sammy.malum.registry.common.entity.MalumEntities.SCYTHE_BOOMERANG.get(),
                                     DiplopiaScytheBoomerangRenderer::new);
    }

    /**
     * 亚大伯斯之石像那口紫色灵魂火：贴图沿用原版 {@code soul_fire_flame}，只染上雕像自己的紫色。
     */
    @SubscribeEvent
    public static void registerParticleProviders(
            net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                org.brahypno.maledict.registry.MaledictParticles.PURPLE_SOUL_FIRE.get(),
                org.brahypno.maledict.client.entity.demiurge.PurpleSoulFireParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FirstVicissitudeBossRenderer.LAYER,
                                      FirstVicissitudeBossModel::createBodyLayer);
        event.registerLayerDefinition(AgeOfEnlightenmentModel.LAYER,
                                      AgeOfEnlightenmentModel::createBodyLayer);
        event.registerLayerDefinition(RavenModel.LAYER, RavenModel::createBodyLayer);
        event.registerLayerDefinition(org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel.HELMET_LAYER,
                                      () -> org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel
                                              .createHelmetLayer(Minecraft.getInstance().getResourceManager()));
        event.registerLayerDefinition(org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel.CHESTPLATE_LAYER,
                                      org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel::createChestplateLayer);
        event.registerLayerDefinition(org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel.CHESTPLATE_DETAILS_LAYER,
                                      () -> org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel
                                              .createChestplateDetailsLayer(Minecraft.getInstance().getResourceManager()));
        event.registerLayerDefinition(org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel.LOWER_ARMOR_LAYER,
                                      org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel::createLowerArmorLayer);
        event.registerLayerDefinition(org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel.BOOTS_LAYER,
                                      () -> org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorModel
                                              .createBootsLayer(Minecraft.getInstance().getResourceManager()));
    }

    /**
     * 扬弃逐光护甲的客户端扩展：1.21 没有 {@code Item#initializeClient}，要在 MOD 总线上按物品登记。
     */
    @SubscribeEvent
    public static void registerClientExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(
                org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorClientExtensions.INSTANCE,
                MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(),
                MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(),
                MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        event.registerItem(
                org.brahypno.maledict.client.item.weapon.baron.BaronStaffClientExtensions.INSTANCE,
                MaledictItems.BARON_CEREMONIAL_STAFF.get());
    }

    /**
     * 裂隙花的刀痕贴图可用性与 RenderType 缓存要随资源重载一起失效。
     */
    @SubscribeEvent
    public static void registerReloadListeners(
            net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                new org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftReloadListener());
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers event) {
        ageOfEnlightenmentModel = new AgeOfEnlightenmentModel(
                event.getEntityModels().bakeLayer(AgeOfEnlightenmentModel.LAYER));
        org.brahypno.maledict.client.item.equipment.lightward.LightwardArmorClientExtensions
                .bake(event.getEntityModels());
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null){
                renderer.addLayer(new RavenShoulderLayer(renderer, event.getEntityModels()));
            }
        }
    }

    /**
     * 面具模型实例，在 {@code AddLayers} 阶段烘焙。
     */
    public static AgeOfEnlightenmentModel ageOfEnlightenmentModel() {
        return ageOfEnlightenmentModel;
    }

    /**
     * Curios 在 AddLayers 时冻结渲染器表，必须在客户端初始化阶段完成绑定。
     */
    @SubscribeEvent
    public static void registerCurioRenderers(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(
                MaledictItems.AGE_OF_ENLIGHTENMENT.get(), AgeOfEnlightenmentCurioRenderer::new));
    }

    /**
     * 自制联机特效类型要在任何一发包到达之前进 Lodestone 的效果表：客户端这里登记一次，
     * 服务端那边由首次派发时的 {@code instance()} 懒登记。
     */
    @SubscribeEvent
    public static void registerNetworkedParticleEffects(FMLClientSetupEvent event) {
        event.enqueueWork(LumenfallRadianceEffect::register);
    }

    /**
     * 两条延迟池显示叠在原版红心之后、护甲之前，不占新的 HUD 行。
     */
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "delayed_vitals_heal"),
                            DelayedVitalsOverlay.PENDING_HEAL);
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "delayed_vitals_damage"),
                            DelayedVitalsOverlay.PENDING_DAMAGE);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0)
                return 0xFFFFFFFF;
            InvocationDefinition definition = InvocationDefinition.find(Minecraft.getInstance().level, stack);
            return definition == null ? 0xFFFFFFFF : 0xFF000000 | definition.tint();
        }, MaledictItems.INVOCATION_SIGIL.get());
        event.register((stack, tintIndex) -> {
                           if (tintIndex != 0 || !(stack.getItem() instanceof SpiritArrowItem arrowItem)){
                               return 0xFFFFFFFF;
                           }
                           return arrowItem.getDefiningSpiritType().getItemColor().getRGB();
                       },
                       MaledictItems.SACRED_SPIRIT_ARROW.get(),
                       MaledictItems.WICKED_SPIRIT_ARROW.get(),
                       MaledictItems.ARCANE_SPIRIT_ARROW.get(),
                       MaledictItems.ELDRITCH_SPIRIT_ARROW.get(),
                       MaledictItems.AERIAL_SPIRIT_ARROW.get(),
                       MaledictItems.AQUEOUS_SPIRIT_ARROW.get(),
                       MaledictItems.EARTHEN_SPIRIT_ARROW.get(),
                       MaledictItems.INFERNAL_SPIRIT_ARROW.get());
    }

    @SubscribeEvent
    public static void registerItemParticleEmitters(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ParticleEmitterHandler.registerItemParticleEmitter(
                    VOID_PARTICLES,
                    MaledictItems.INCURSUS_BLADE.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                    MaledictItems.EMPEROR_IN_THE_EGGSHELL.get());
            ParticleEmitterHandler.registerItemParticleEmitter(
                    ESOTERICA_PARTICLES,
                    MaledictItems.ELEGY_BOW.get(), MaledictItems.BLEEDING_EYE.get(), MaledictItems.HANGMAN_FOOL.get(),
                    MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get(), MaledictItems.LIGHTFALL_CODEX.get());
        });
    }

    /**
     * 常驻后处理器只登记一次；Lodestone 按 isActive 执行，首次生效时才载入链。
     */
    @SubscribeEvent
    public static void registerPostProcessor(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PostProcessHandler.addInstance(InfraredPostProcessor.INSTANCE);
            PostProcessHandler.addInstance(VicissitudeSummoningPostProcessor.INSTANCE);
        });
    }

    private MaledictClientSetup() {
    }
}
