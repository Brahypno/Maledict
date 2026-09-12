package org.brahypno.maledict.client;

import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.IncursusBladeItem;

import java.text.DecimalFormat;
import java.util.List;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class IncursusBladeTooltipHandler {
    private IncursusBladeTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof IncursusBladeItem)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        addDescription(event, tooltip);

        String magicDamageName = Component.translatable("attribute.name.lodestone.magic_damage").getString();
        int insertionIndex = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().contains(magicDamageName)) {
                insertionIndex = i + 1;
                break;
            }
        }

        double damage = IncursusBladeItem.getStat(
                event.getItemStack(), IncursusBladeItem.POWDER_SNOW_DAMAGE);
        String formattedDamage = new DecimalFormat("#.##").format(damage);
        Component line = Component.literal(" ").append(Component.translatable(
                "attribute.modifier.equals.0",
                formattedDamage,
                Component.translatable("attribute.name.maledict.powder_snow_damage")))
                .withStyle(ChatFormatting.DARK_GREEN);
        tooltip.add(insertionIndex, line);
    }

    private static void addDescription(ItemTooltipEvent event, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());
        tooltip.add(index++, Component.translatable("tooltip.maledict.incursus_blade.description")
                .withStyle(ChatFormatting.GRAY));
        if (!IncursusBladeItem.hasAllStatsAtLeast(
                event.getItemStack(), IncursusBladeItem.MEDIUM_DAMAGE_LEVEL)) {
            tooltip.add(index++, Component.translatable("tooltip.maledict.incursus_blade.medium_unlock_hint")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(index, Component.translatable("tooltip.maledict.incursus_blade.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(index++, Component.translatable("tooltip.maledict.incursus_blade.infusion")
                .withStyle(ChatFormatting.GRAY));
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.EARTHEN_SPIRIT,
                IncursusBladeItem.ATTACK_DAMAGE, "earthen");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.AQUEOUS_SPIRIT,
                IncursusBladeItem.POWDER_SNOW_DAMAGE, "aqueous");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.ARCANE_SPIRIT,
                IncursusBladeItem.MAGIC_DAMAGE, "arcane");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.AERIAL_SPIRIT,
                IncursusBladeItem.AERIAL_PROGRESS, "aerial");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.SACRED_SPIRIT,
                IncursusBladeItem.SACRED_POWER, "sacred");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.INFERNAL_SPIRIT,
                IncursusBladeItem.INFERNAL_POWER, "infernal");
        index = addSpiritLine(event, tooltip, index, SpiritTypeRegistry.ELDRITCH_SPIRIT,
                IncursusBladeItem.ELDRITCH_ABSORPTION, "eldritch");
        addSpiritLine(event, tooltip, index, SpiritTypeRegistry.WICKED_SPIRIT,
                IncursusBladeItem.WICKED_CRITICAL_DAMAGE, "wicked");
    }

    private static int addSpiritLine(ItemTooltipEvent event,
                                     List<Component> tooltip,
                                     int index,
                                     MalumSpiritType spiritType,
                                     String statKey,
                                     String translationSuffix) {
        double value = IncursusBladeItem.getStat(event.getItemStack(), statKey);
        int progress = IncursusBladeItem.getUpgradeProgress(event.getItemStack(), statKey);
        int cost = IncursusBladeItem.getNextUpgradeCost(event.getItemStack(), statKey);
        Component line = Component.translatable(
                "tooltip.maledict.incursus_blade.spirit." + translationSuffix,
                format(value), progress, cost)
                .withStyle(style -> style.withColor(spiritType.getTextColor(false)));
        tooltip.add(index, line);
        return index + 1;
    }

    private static String format(double value) {
        return new DecimalFormat("#.##").format(value);
    }
}
