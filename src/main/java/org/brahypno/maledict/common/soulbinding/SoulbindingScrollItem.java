package org.brahypno.maledict.common.soulbinding;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Teaches the Rite of Soulbinding.
 *
 * <p>The first use grants {@code maledict:soulbinding_rite} and spends one scroll. Once the player
 * knows the rite, use is free and only reports that fact. Acquiring the scroll is intentionally out
 * of scope; converting a spare scroll back into three paper is an ordinary crafting recipe.
 */
public final class SoulbindingScrollItem extends Item {
    public SoulbindingScrollItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return level.isClientSide
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.consume(stack);
        }

        if (Soulbinding.knows(serverPlayer)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding.already_known"), true);
            return InteractionResultHolder.consume(stack);
        }

        if (Soulbinding.award(serverPlayer)) {
            stack.shrink(1);
            player.getInventory().setChanged();
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding.learned"), true);
        }
        return InteractionResultHolder.consume(stack);
    }
}
