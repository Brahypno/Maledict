package org.brahypno.maledict.common.equipment.lightward;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** 扬弃逐光护甲；套装 tooltip 也写在这里。 */
public final class LightwardArmorItem extends ArmorItem {
    private static final String SET_KEY = "tooltip.maledict.aufheben_lightward.set";
    private static final String COMPOSITION_KEY = "tooltip.maledict.aufheben_lightward.composition";
    private static final String COMPOSITION_ARMOR_KEY =
            "tooltip.maledict.aufheben_lightward.composition.armor";
    private static final String COMPOSITION_WEAPONS_KEY =
            "tooltip.maledict.aufheben_lightward.composition.weapons";
    private static final String EFFECT_4_KEY = "tooltip.maledict.aufheben_lightward.effect.4";
    private static final String EFFECT_5_KEY = "tooltip.maledict.aufheben_lightward.effect.5";

    /** 已激活：亮蓝紫；未激活：暗灰。 */
    private static final Style ACTIVE_STYLE = Style.EMPTY.withColor(0x9B7BFF);
    private static final Style INACTIVE_STYLE = Style.EMPTY.applyFormat(ChatFormatting.DARK_GRAY);

    public LightwardArmorItem(Type type, Item.Properties properties) {
        super(LightwardArmorMaterial.INSTANCE, type, properties);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return LightwardSet.boostedEnchantmentLevel(stack, enchantment);
    }

    @Override
    public Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        return LightwardSet.boostedEnchantments(stack);
    }

    @Override
    public int getDefaultTooltipHideFlags(ItemStack stack) {
        return ItemStack.TooltipPart.ENCHANTMENTS.getMask();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        int pieces = LightwardSet.viewerPieces();
        tooltip.add(Component.translatable(SET_KEY, pieces)
                            .withStyle(ChatFormatting.DARK_PURPLE));
        if (FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown()) {
            addSetDetails(tooltip, pieces);
        }
        LightwardSet.appendEnchantments(stack, tooltip);
    }

    private static void addSetDetails(List<Component> tooltip, int pieces) {
        tooltip.add(Component.translatable(COMPOSITION_KEY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(COMPOSITION_ARMOR_KEY)
                            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(COMPOSITION_WEAPONS_KEY)
                            .withStyle(ChatFormatting.DARK_GRAY));
        addEffect(tooltip, EFFECT_4_KEY, pieces >= LightwardSet.ENCHANT_THRESHOLD);
        addEffect(tooltip, EFFECT_5_KEY, LightwardSet.viewerMainhandUnlocked());
    }

    private static void addEffect(List<Component> tooltip, String key, boolean active) {
        tooltip.add(Component.translatable(key).withStyle(active ? ACTIVE_STYLE : INACTIVE_STYLE));
    }
}
