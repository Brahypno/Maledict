package org.brahypno.maledict.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.common.curio.VicissitudeCurioReturns;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Claim token for confiscated Curios that could not be delivered automatically.
 * <p>The token is a ticket, never the storage: the stacks stay on the server side ledger until they
 * are really equipped or placed, so a lost or duplicated token can never duplicate an item.
 */
public final class CurioReturnTokenItem extends Item {
    public CurioReturnTokenItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        VicissitudeCurioReturns.claim(serverPlayer);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.maledict.curio_return_token"));
    }
}
