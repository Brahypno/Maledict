package org.brahypno.maledict.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.compat.tetra.TetraCompat;

import java.util.List;

/**
 * 给「效果在、但强化列不出来」的 Tetra 工具补一行说明。
 *
 * <p>Tetra 的工具提示只认两样：它自己注册过的 stat 条，和模块的强化名（{@code tetra.improvement.*}）。
 * 反照在**剑身**上是靠强化（宝石材料的 {@code improvements}）列出来的；而**插槽**用的
 * {@code sword/socket} 模块在自己的模块 JSON 里没有 {@code improvements} 字段，所以镶嵌进去的
 * 反照不会有任何提示 —— 这是 Tetra 的数据限制，不是我们的效果没生效。
 *
 * <p>这里在 {@code ItemTooltipEvent} 里补一行：只有当这件工具确实带该效果、而强化列表里<b>没有</b>它时
 * 才加，避免和 Tetra 自己那行重复。该事件在 {@code appendHoverText} 之后触发，所以把行插在名字下面
 * （索引 1），不会被塞到提示最底部。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModularAbilityTooltipEvents {
    /** 效果键、强化键与提示行翻译键；两能力对称处理。 */
    private static final AbilityLine[] ABILITIES = {
            new AbilityLine("maledict.refulgence", "tooltip.maledict.refulgence.line"),
            new AbilityLine("maledict.lumenfall", "tooltip.maledict.lumenfall.line")
    };

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !TetraCompat.loaded()) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        for (AbilityLine ability : ABILITIES) {
            int level = TetraCompat.effectLevel(stack, ability.effectKey());
            if (level <= 0 || TetraCompat.hasImprovement(stack, ability.effectKey())) {
                // 没这个效果，或者 Tetra 已经用强化名列出来了（剑身那种），就不重复贴。
                continue;
            }
            tooltip.add(Math.min(1, tooltip.size()),
                        Component.translatable(ability.translationKey(), level)
                                 .withStyle(ChatFormatting.GRAY));
        }
    }

    private record AbilityLine(String effectKey, String translationKey) {
    }

    private ModularAbilityTooltipEvents() {
    }
}
