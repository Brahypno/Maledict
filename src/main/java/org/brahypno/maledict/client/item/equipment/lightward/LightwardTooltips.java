package org.brahypno.maledict.client.item.equipment.lightward;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.equipment.lightward.LightwardSet;
import org.brahypno.maledict.config.MaledictConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 扬弃逐光套装的 tooltip。
 *
 * <p>1.20.1 用 {@code getDefaultTooltipHideFlags} 只藏原版附魔行，套装行与增幅后的附魔行写在
 * {@code appendHoverText} 里。1.21.1 没有这种细粒度的隐藏位（{@code HIDE_ADDITIONAL_TOOLTIP}
 * 只挡 {@code appendHoverText}，而 {@code ItemStack#getTooltipLines} 里的
 * {@code addToTooltip(DataComponents.ENCHANTMENTS)} 在它之外无条件执行），所以整套文案改在
 * {@code ItemTooltipEvent} 里补：先用 {@link LightwardSet#stripVanillaEnchantments} 按原始等级
 * 重算出原版那几行并摘掉，再插入自己的行，最终每个附魔只留一行「名 IV（II+II）」。
 * 顺序与 1.20.1 一致：物品名、套装行、附魔行，之后才是 NeoForge 的属性行。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class LightwardTooltips {
    private static final String SET_KEY = "tooltip.maledict.aufheben_lightward.set";
    private static final String COMPOSITION_KEY = "tooltip.maledict.aufheben_lightward.composition";
    private static final String COMPOSITION_ARMOR_KEY =
            "tooltip.maledict.aufheben_lightward.composition.armor";
    private static final String COMPOSITION_WEAPONS_KEY =
            "tooltip.maledict.aufheben_lightward.composition.weapons";
    private static final String EFFECT_2_KEY = "tooltip.maledict.aufheben_lightward.effect.2";
    private static final String EFFECT_3_KEY = "tooltip.maledict.aufheben_lightward.effect.3";
    private static final String EFFECT_4_KEY = "tooltip.maledict.aufheben_lightward.effect.4";
    private static final String EFFECT_5_KEY = "tooltip.maledict.aufheben_lightward.effect.5";

    /**
     * 已激活：亮蓝紫；未激活：暗灰。
     */
    private static final Style ACTIVE_STYLE = Style.EMPTY.withColor(0x9B7BFF);
    private static final Style INACTIVE_STYLE = Style.EMPTY.applyFormat(ChatFormatting.DARK_GRAY);

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!LightwardSet.isSetPiece(stack)) {
            return;
        }
        List<Component> lines = new ArrayList<>(6);
        // 1.20.1 的套装块写在 LightwardArmorItem#appendHoverText 里：只有护甲印标题、合成行与四条效果，
        // 三把套武器只补附魔行（原注释「套装文字只在护甲上；这里只补附魔」）。
        if (LightwardSet.isArmor(stack)) {
            int pieces = LightwardClientState.viewerPieces();
            lines.add(Component.translatable(SET_KEY, pieces).withStyle(ChatFormatting.DARK_PURPLE));
            if (Screen.hasShiftDown()) {
                addSetDetails(lines, pieces);
            }
        }
        LightwardSet.appendEnchantments(stack, lines);
        // 先摘掉原版随 ENCHANTMENTS 组件写下的那几行，再插入自己的，否则同一附魔会留下两行。
        List<Component> tooltip = event.getToolTip();
        LightwardSet.stripVanillaEnchantments(stack, tooltip);
        if (!lines.isEmpty()) {
            tooltip.addAll(Math.min(1, tooltip.size()), lines);
        }
    }

    private static void addSetDetails(List<Component> tooltip, int pieces) {
        tooltip.add(Component.translatable(COMPOSITION_KEY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(COMPOSITION_ARMOR_KEY).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(COMPOSITION_WEAPONS_KEY).withStyle(ChatFormatting.DARK_GRAY));
        boolean warded = LightwardClientState.viewerHasSoulWard();
        addEffect(tooltip, EFFECT_2_KEY, pieces >= LightwardSet.SPEED_THRESHOLD && warded);
        addEffect(tooltip, EFFECT_3_KEY, pieces >= LightwardSet.ABNEGATION_THRESHOLD && warded,
                  abnegationPercent());
        addEffect(tooltip, EFFECT_4_KEY,
                  pieces >= LightwardSet.ENCHANT_THRESHOLD && LightwardClientState.viewerSoulWardFill() > 0.0D);
        addEffect(tooltip, EFFECT_5_KEY, LightwardClientState.viewerMainhandUnlocked());
    }

    private static int abnegationPercent() {
        return (int) Math.round(MaledictConfig.LIGHTWARD_ABNEGATION_REDUCTION.get() * 100.0D);
    }

    private static void addEffect(List<Component> tooltip, String key, boolean active, Object... args) {
        tooltip.add(Component.translatable(key, args).withStyle(active ? ACTIVE_STYLE : INACTIVE_STYLE));
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    private LightwardTooltips() {
    }
}
