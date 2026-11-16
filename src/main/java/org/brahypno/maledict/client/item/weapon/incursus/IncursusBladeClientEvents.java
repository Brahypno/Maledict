package org.brahypno.maledict.client.item.weapon.incursus;

import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.network.MaledictNetwork;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 神侵恶刃的客户端交互与物品提示。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncursusBladeClientEvents {

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack()
            || event.getHand() != InteractionHand.MAIN_HAND
            || minecraft.player == null
            || !(minecraft.player.getMainHandItem().getItem() instanceof IncursusBladeItem)){
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(true);
        MaledictNetwork.sendRadialAttack();
        minecraft.player.resetAttackStrengthTicker();
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof IncursusBladeItem)){
            return;
        }

        List<Component> tooltip = event.getToolTip();
        addDescription(event, tooltip);
        // 冻结属性已经是主手属性修饰符，原版属性 tooltip 会自己列出；
        // 这里不再手写一行，避免同一条修饰符出现两次。
    }

    private static void addDescription(ItemTooltipEvent event, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());
        tooltip.add(index++, Component.translatable("tooltip.maledict.incursus_blade.description")
                                      .withStyle(ChatFormatting.GRAY));
        if (!IncursusBladeItem.hasAllStatsAtLeast(
                event.getItemStack(), IncursusBladeItem.MEDIUM_DAMAGE_LEVEL)){
            tooltip.add(index++, Component.translatable("tooltip.maledict.incursus_blade.medium_unlock_hint")
                                          .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (!Screen.hasShiftDown()){
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

    private static int addSpiritLine(
            ItemTooltipEvent event,
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

    private IncursusBladeClientEvents() {
    }
}
