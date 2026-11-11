package org.brahypno.maledict.common.soulbinding;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

import java.util.ArrayList;
import java.util.List;

/**
 * Tooltip half of the soulbinding record.
 *
 * <p>An armed tool shows the empty seal and the promise of the next kill. A bound tool shows exactly
 * one spirit name — the individual custom name when valid, otherwise the localized entity type,
 * otherwise the registry id. The borrowed numbers are not printed twice: Forge already renders the
 * main-hand attribute modifiers added by {@link SoulbindingAttributes} in the item's normal
 * "When in Main Hand" section, so this listener only adds the state and the single spirit name.
 * Everything is read from the stack snapshot, so the tooltip does not need the killed entity to
 * still exist.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingTooltips {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SoulbindingData.State state = SoulbindingData.state(stack);
        if (state == SoulbindingData.State.EMPTY) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        List<Component> lines = new ArrayList<>(3);
        if (state == SoulbindingData.State.ARMED) {
            lines.add(Component.translatable("tooltip.maledict.soulbinding.armed")
                               .withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tooltip.maledict.soulbinding.vacant")
                               .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.maledict.soulbinding.next_kill")
                               .withStyle(ChatFormatting.GRAY));
        } else {
            SoulbindingData.Snapshot snapshot = SoulbindingData.snapshot(stack);
            if (snapshot == null) {
                return;
            }
            lines.add(Component.translatable("tooltip.maledict.soulbinding.bound")
                               .withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tooltip.maledict.soulbinding.spirit",
                                             SoulbindingData.displayName(snapshot)));
        }
        // Keep the item name on top, then the soulbinding state/name, then the vanilla attribute
        // lines. The borrowed numbers are rendered once by Forge's normal main-hand section.
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private SoulbindingTooltips() {
    }
}
