package org.brahypno.maledict.client.item;

import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.network.MaledictNetwork;

import java.text.DecimalFormat;
import java.util.List;

/** 神侵恶刃的客户端交互与物品提示。 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class IncursusBladeClientEvents {

    @SubscribeEvent
    public static void hideAbsorptionCapacityTooltip(GatherSkippedAttributeTooltipsEvent event) {
        if (event.getStack().getItem() instanceof IncursusBladeItem) {
            event.skipId(IncursusBladeItem.ABSORPTION_CAPACITY_MODIFIER_ID);
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack()
                || event.getHand() != InteractionHand.MAIN_HAND
                || minecraft.player == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof IncursusBladeItem)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(true);
        MaledictNetwork.sendRadialAttack();
        minecraft.player.resetAttackStrengthTicker();
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
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.EARTHEN_SPIRIT,
                IncursusBladeItem.ATTACK_DAMAGE, "earthen");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.AQUEOUS_SPIRIT,
                IncursusBladeItem.POWDER_SNOW_DAMAGE, "aqueous");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.ARCANE_SPIRIT,
                IncursusBladeItem.MAGIC_DAMAGE, "arcane");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.AERIAL_SPIRIT,
                IncursusBladeItem.AERIAL_PROGRESS, "aerial");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.SACRED_SPIRIT,
                IncursusBladeItem.SACRED_POWER, "sacred");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.INFERNAL_SPIRIT,
                IncursusBladeItem.INFERNAL_POWER, "infernal");
        index = addSpiritLine(event, tooltip, index, MalumSpiritTypes.ELDRITCH_SPIRIT,
                IncursusBladeItem.ELDRITCH_ABSORPTION, "eldritch");
        addSpiritLine(event, tooltip, index, MalumSpiritTypes.WICKED_SPIRIT,
                IncursusBladeItem.WICKED_CRITICAL_DAMAGE, "wicked");
    }

    private static int addSpiritLine(ItemTooltipEvent event,
                                     List<Component> tooltip,
                                     int index,
                                     com.sammy.malum.core.systems.spirit.type.SpiritLike spiritType,
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

    private IncursusBladeClientEvents() {
    }
}
