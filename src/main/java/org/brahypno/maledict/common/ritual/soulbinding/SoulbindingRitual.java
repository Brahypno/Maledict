package org.brahypno.maledict.common.ritual.soulbinding;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictItems;
import org.jetbrains.annotations.Nullable;

/**
 * Activation-time half of the Rite of Soulbinding.
 *
 * <p>Malum activates a totem when a player right-clicks its base, and that click is the only point
 * where the initiating player is exposed to a rite. The checks therefore run here, before Malum
 * starts assembling: Soulwood arms the main-hand tool, Runewood releases it. The rite itself is
 * still registered as one rite with both wood variants; its effects are intentionally dormant.
 *
 * <p>Every failure is reported with a short message and leaves both hands untouched. On success the
 * base is activated through its normal {@code onUseWithItem} path, so the totem assembles and lights
 * up exactly like any other one-time rite.
 *
 * <p>1.21 侧的两点结构性差异（照 {@code InvocationEvents} 的写法处理）：站点状态读
 * {@code getState()/isActiveOrAssembling()}（字段是 protected），木材读公开的 {@code corrupted}；
 * 「这个 totem 是不是我的仪式」由 {@link SoulbindingRite#inspect} 直接扫柱子判定。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingRitual {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND){
            return;
        }
        if (event.getUseBlock() == TriState.FALSE || event.getUseItem() == TriState.FALSE){
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof TotemBaseBlockEntity base)){
            return;
        }
        if (base.isActiveOrAssembling()){
            return;
        }
        if (SoulbindingRite.inspect(event.getLevel(), base) == null){
            return;
        }

        // This click belongs to the Soulbinding totem. Cancel on both sides so the client never
        // predicts assembling a rite the server may refuse; the server then performs the checks
        // and starts the assembly itself.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof ServerPlayer player)){
            return;
        }
        if (player.isSpectator()){
            return;
        }
        if (!player.mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())){
            player.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding.cannot_use"), true);
            return;
        }

        String error = base.corrupted ? arm(player) : release(player);
        if (error != null){
            player.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding." + error), true);
            return;
        }
        base.onUseWithItem(player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    /**
     * Soulwood: EMPTY becomes ARMED and exactly one offhand seal is spent.
     */
    @Nullable
    static String arm(ServerPlayer player) {
        if (!Soulbinding.knows(player)){
            return "no_advancement";
        }
        ItemStack main = player.getMainHandItem();
        if (!isSoulBindable(main)){
            return "invalid_tool";
        }
        if (SoulbindingData.state(main) != SoulbindingData.State.EMPTY){
            return "already_armed";
        }
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty() || !off.is(MaledictItems.SOULBINDING_SEAL.get())){
            return "no_seal";
        }

        SoulbindingData.arm(main);
        off.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return null;
    }

    /**
     * Runewood: ARMED or BOUND becomes EMPTY. Nothing is refunded.
     */
    @Nullable
    static String release(ServerPlayer player) {
        if (!Soulbinding.knows(player)){
            return "no_advancement";
        }
        ItemStack main = player.getMainHandItem();
        if (!isSoulBindable(main)){
            return "invalid_tool";
        }
        SoulbindingData.State state = SoulbindingData.state(main);
        if (state != SoulbindingData.State.ARMED && state != SoulbindingData.State.BOUND){
            return "not_armed";
        }
        SoulbindingData.clear(main);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return null;
    }

    private static boolean isSoulBindable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MaledictTags.SOUL_BINDABLE)
               && !stack.is(MaledictTags.SOUL_BINDING_TOOL_BLACKLIST);
    }

    private SoulbindingRitual() {
    }
}
