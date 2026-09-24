package org.brahypno.maledict.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.IncursusBladeBlink;
import org.brahypno.maledict.common.item.IncursusBladeItem;
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
            registerBlinkProperty(MaledictItems.INCURSUS_BLADE.get());
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

    /**
     * 眨眼：置 {@code 1} 的那几帧，模型整份换成闭眼贴图（见 {@code MaledictItemModels}）。
     *
     * <p>节奏分三档，都在 {@link IncursusBladeBlink} 里；这里只回答「这一帧它和玩家是什么关系」。
     */
    private static void registerBlinkProperty(Item item) {
        ItemProperties.register(item, IncursusBladeItem.BLINK_PROPERTY,
                (stack, level, entity, seed) -> IncursusBladeBlink.blinkValue(
                        IncursusBladeBlink.seedOf(stack), gazeOf(stack), blinkClock()));
    }

    /**
     * 这一帧这把刃和玩家的关系。
     *
     * <p>和启蒙之年那个谓词一样，看的是<b>本地玩家</b>：这是「我现在怎么看着它」，不是
     * 「这个物品属于谁」。渲染者根本拿不到（物品栏里的物品走的正是这一条：GUI 渲染给过来的
     * {@code entity}/{@code level} 都是空的），所以只认 {@code Minecraft.getInstance()}；
     * 本地玩家也没了（主菜单之类）就只剩「躺在背包里」这一档。
     *
     * <p>优先级：鼠标压过快捷栏。<b>悬停</b>既包括鼠标停在这一格上，也包括鼠标上正拿着它
     * （那一份不在任何格子里，{@code getSlotUnderMouse()} 是看不到的）；再往下才轮到
     * 「它在快捷栏选中那一格里」——也就是当前主手的那一份，{@code ItemStack} 是同一个实例，
     * 所以直接比引用就够了。
     */
    private static IncursusBladeBlink.Gaze gazeOf(ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return IncursusBladeBlink.Gaze.IDLE;
        }
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            Slot hovered = screen.getSlotUnderMouse();
            if (hovered != null && hovered.getItem() == stack) {
                return IncursusBladeBlink.Gaze.HOVERED;
            }
        }
        if (player.containerMenu.getCarried() == stack) {
            return IncursusBladeBlink.Gaze.HOVERED;
        }
        return player.getMainHandItem() == stack
                ? IncursusBladeBlink.Gaze.SELECTED
                : IncursusBladeBlink.Gaze.IDLE;
    }

    /**
     * 眨眼用的时钟：世界时间优先，没有世界就退到真实时间的 20 tick/秒。
     *
     * <p>时钟单调、跟渲染的是哪个界面无关，同一把刃在背包、快捷栏、创造模式列表里
     * 眨的是同一个节奏。用世界时间而不是「打开界面之后的第几帧」，是为了让节奏在
     * 关掉再打开物品栏之后接着走，不会每次重开都从头开始眨。
     */
    private static long blinkClock() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : Util.getMillis() / 50L;
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
