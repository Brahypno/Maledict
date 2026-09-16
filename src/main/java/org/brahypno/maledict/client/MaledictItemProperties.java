package org.brahypno.maledict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.RemembranceBowItem;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictMobEffects;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictItemProperties {
    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties(MaledictItems.REMEMBRANCE_BOW.get());
            registerBowProperties(MaledictItems.ELEGY_BOW.get());
            registerEnlightenedProperty(MaledictItems.AGE_OF_ENLIGHTENMENT.get());
        });
    }

    /**
     * 佩戴者身上有启蒙之年时把谓词置 {@code 1}，模型据此切到第二张贴图。
     *
     * <p>判断的是**本地玩家**：这是一个「我现在是什么状态」的谓词，不是「这个物品属于谁」。
     * 渲染者不是玩家时（箱子、展示框）退回本地玩家，本地玩家也为空才返回 {@code 0}，
     * 所以那些地方看到的始终是原贴图。
     */
    private static void registerEnlightenedProperty(Item item) {
        ItemProperties.register(item, AgeOfEnlightenmentItem.ENLIGHTENED_PROPERTY,
                (stack, level, entity, seed) -> {
                    Player player = entity instanceof Player holder ? holder : Minecraft.getInstance().player;
                    return player != null && player.hasEffect(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get())
                            ? 1.0F : 0.0F;
                });
    }

    private static void registerBowProperties(Item item) {
        ItemProperties.register(item, ResourceLocation.fromNamespaceAndPath("minecraft", "pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack
                            || !(stack.getItem() instanceof RemembranceBowItem bow)) {
                        return 0.0F;
                    }
                    int usedTicks = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                    return Mth.clamp(
                            usedTicks * bow.getDrawSpeedMultiplier(stack) / BowItem.MAX_DRAW_DURATION,
                            0.0F, 1.0F);
                });
        ItemProperties.register(item, ResourceLocation.fromNamespaceAndPath("minecraft", "pulling"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private MaledictItemProperties() {
    }
}
