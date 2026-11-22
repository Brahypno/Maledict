package org.brahypno.maledict.common.item.codex;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.network.MaledictNetwork;

/**
 * 坠光秘典：右键打开法典。
 *
 * <p>与 Malum 1.8.2 的两本全典不同，这一件**必须**经服务端回一趟——「已学会拘魂仪式」是成就状态，
 * 只有服务端知道，而「学会了才显示虚空页」正是本模组法典的一半行为。所以客户端一句 {@code use}
 * 不自己开屏，等 {@code OpenLightfallCodexPacket} 回来再开；返回值照 1.20.1 用
 * {@code sidedSuccess}，客户端拿 SUCCESS、服务端拿 CONSUME，不会双开。
 */
public final class LightfallCodexItem extends Item {
    public LightfallCodexItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            MaledictNetwork.openLightfallCodex(serverPlayer);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 1.21 的 isClientSide 是 Level 的字段，不再是方法。
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
