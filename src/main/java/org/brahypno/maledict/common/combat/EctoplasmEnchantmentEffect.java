package org.brahypno.maledict.common.combat;

import com.sammy.malum.registry.common.item.ItemTagRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictEnchantments;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EctoplasmEnchantmentEffect {
    private static final double SPEED_INCREASE_PER_LEVEL = 0.08D;
    private static final String PROCESSED_TAG = Maledict.MODID + ":ectoplasm_processed";

    @SubscribeEvent
    public static void increaseArrowSpeed(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
            || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof Player player)
            || arrow.getPersistentData().getBoolean(PROCESSED_TAG)){
            return;
        }
        arrow.getPersistentData().putBoolean(PROCESSED_TAG, true);

        ItemStack bow = getUsedBow(player);
        if (bow.isEmpty()){
            return;
        }

        int level = bow.getEnchantmentLevel(MaledictEnchantments.ECTOPLASM.get());
        if (level <= 0){
            return;
        }

        int spiritCost = (level + 1) / 2;
        if (!hasEnoughSpirits(player, spiritCost)){
            return;
        }

        if (!player.getAbilities().instabuild){
            consumeSpirits(player, spiritCost);
        }

        double multiplier = 1.0D + SPEED_INCREASE_PER_LEVEL * level;
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(multiplier));
    }

    private static boolean hasEnoughSpirits(Player player, int required) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ItemTagRegistry.ASPECTED_SPIRITS)){
                found += stack.getCount();
                if (found >= required){
                    return true;
                }
            }
        }
        return false;
    }

    private static void consumeSpirits(Player player, int amount) {
        for (int slot = 0; slot < player.getInventory().getContainerSize() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(ItemTagRegistry.ASPECTED_SPIRITS)){
                continue;
            }

            int consumed = Math.min(amount, stack.getCount());
            stack.shrink(consumed);
            amount -= consumed;
        }
        player.getInventory().setChanged();
    }

    private static ItemStack getUsedBow(Player player) {
        ItemStack useItem = player.getUseItem();
        if (useItem.getItem() instanceof BowItem){
            return useItem;
        }

        InteractionHand usedHand = player.getUsedItemHand();
        ItemStack heldItem = player.getItemInHand(usedHand);
        if (heldItem.getItem() instanceof BowItem){
            return heldItem;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof BowItem){
            return mainHandItem;
        }

        ItemStack offhandItem = player.getOffhandItem();
        return offhandItem.getItem() instanceof BowItem ? offhandItem : ItemStack.EMPTY;
    }

    private EctoplasmEnchantmentEffect() {
    }
}
