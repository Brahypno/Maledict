package org.brahypno.maledict.client;

import com.sammy.malum.common.item.IVoidItem;
import com.sammy.malum.visual_effects.ScreenParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.curio.enlightenment.AgeOfEnlightenmentCurioRenderer;
import org.brahypno.maledict.client.curio.enlightenment.AgeOfEnlightenmentModel;
import org.brahypno.maledict.client.curio.hangman.HangmanTrapRenderer;
import org.brahypno.maledict.client.infrared.InfraredPostProcessor;
import org.brahypno.maledict.client.raven.RavenFeatherRenderer;
import org.brahypno.maledict.client.raven.RavenModel;
import org.brahypno.maledict.client.raven.RavenRenderer;
import org.brahypno.maledict.client.raven.RavenShoulderLayer;
import org.brahypno.maledict.client.realm.BlightedRealmEffects;
import org.brahypno.maledict.client.vicissitude.model.FirstVicissitudeBossModel;
import org.brahypno.maledict.client.vicissitude.render.FirstVicissitudeBossRenderer;
import org.brahypno.maledict.client.vicissitude.render.VicissitudeLightOrbRenderer;
import org.brahypno.maledict.client.vicissitude.render.VicissitudeScytheRenderer;
import org.brahypno.maledict.client.vicissitude.render.VicissitudeSpiritBoltRenderer;
import org.brahypno.maledict.client.vicissitude.vfx.VicissitudeSummoningPostProcessor;
import org.brahypno.maledict.client.vitals.DelayedVitalsOverlay;
import org.brahypno.maledict.client.weapon.remembrance.SpiritArrowRenderer;
import org.brahypno.maledict.common.realm.BlightedRealm;
import org.brahypno.maledict.common.invocation.InvocationDefinition;
import org.brahypno.maledict.common.weapon.remembrance.SpiritArrowItem;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.systems.particle.screen.ScreenParticleHolder;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 客户端 MOD 总线注册入口：实体与图层、饰品、HUD、物品颜色与粒子、后处理器。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
        event.registerEntityRenderer(MaledictEntities.HANGMAN_TRAP.get(), HangmanTrapRenderer::new);
        event.registerEntityRenderer(MaledictEntities.SPIRIT_ARROW.get(), SpiritArrowRenderer::new);
        event.registerEntityRenderer(MaledictEntities.RAVEN.get(), RavenRenderer::new);
        event.registerEntityRenderer(MaledictEntities.RAVEN_FEATHER.get(), RavenFeatherRenderer::new);
        event.registerEntityRenderer(MaledictEntities.FIRST_VICISSITUDE.get(), FirstVicissitudeBossRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), VicissitudeLightOrbRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SPIRIT_BOLT.get(), VicissitudeSpiritBoltRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SCYTHE.get(), VicissitudeScytheRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FirstVicissitudeBossRenderer.LAYER,
                                      FirstVicissitudeBossModel::createBodyLayer);
        event.registerLayerDefinition(AgeOfEnlightenmentModel.LAYER,
                                      AgeOfEnlightenmentModel::createBodyLayer);
        event.registerLayerDefinition(RavenModel.LAYER, RavenModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers event) {
        ageOfEnlightenmentModel = new AgeOfEnlightenmentModel(
                event.getEntityModels().bakeLayer(AgeOfEnlightenmentModel.LAYER));
        for (String skin : event.getSkins()) {
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
     * 两条延迟池显示叠在原版红心之后、护甲之前，不占新的 HUD 行。
     */
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "delayed_vitals_heal",
                            DelayedVitalsOverlay.PENDING_HEAL);
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "delayed_vitals_damage",
                            DelayedVitalsOverlay.PENDING_DAMAGE);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
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
                    MaledictItems.INCURSUS_BLADE.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get());
            ParticleEmitterHandler.registerItemParticleEmitter(
                    ESOTERICA_PARTICLES,
                    MaledictItems.ELEGY_BOW.get(), MaledictItems.BLEEDING_EYE.get(), MaledictItems.HANGMAN_FOOL.get());
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
