package org.brahypno.maledict.client;

import net.minecraft.ChatFormatting;
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
}
