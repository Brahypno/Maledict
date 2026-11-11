package org.brahypno.maledict.common.soulbinding;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
 * base is activated through its normal {@code onUse} path, so the totem assembles and lights up
 * exactly like any other one-time rite.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingRitual {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) {
            return;
        }
        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof TotemBaseBlockEntity base)) {
            return;
        }
        if (base.state != TotemBaseBlockEntity.TotemRiteState.IDLE) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos().above()) instanceof TotemPoleBlockEntity)) {
            return;
        }
        if (!SoulbindingRite.matches(level, base)) {
            return;
        }

        // This click belongs to the Soulbinding totem. Cancel on both sides so the client never
        // predicts assembling a rite the server may refuse; the server then performs the checks
        // and starts the assembly itself.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(level instanceof ServerLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        if (!player.mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())) {
            player.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding.cannot_use"), true);
            return;
        }

        String error = base.isSoulwood ? arm(player) : release(player);
        if (error != null) {
            player.displayClientMessage(
                    Component.translatable("message.maledict.soulbinding." + error), true);
            return;
        }
        base.onUse(player, InteractionHand.MAIN_HAND);
    }

    /** Soulwood: EMPTY becomes ARMED and exactly one offhand seal is spent. */
    @Nullable
    static String arm(ServerPlayer player) {
        if (!Soulbinding.knows(player)) {
            return "no_advancement";
        }
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty() || !main.is(MaledictTags.SOUL_BINDABLE)) {
            return "invalid_tool";
        }
        if (SoulbindingData.state(main) != SoulbindingData.State.EMPTY) {
            return "already_armed";
        }
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty() || !off.is(MaledictItems.SOULBINDING_SEAL.get())) {
            return "no_seal";
        }

        SoulbindingData.arm(main);
        off.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return null;
    }

    /** Runewood: ARMED or BOUND becomes EMPTY. Nothing is refunded. */
    @Nullable
    static String release(ServerPlayer player) {
        if (!Soulbinding.knows(player)) {
            return "no_advancement";
        }
        ItemStack main = player.getMainHandItem();
        SoulbindingData.State state = SoulbindingData.state(main);
        if (state != SoulbindingData.State.ARMED && state != SoulbindingData.State.BOUND) {
            return "not_armed";
        }
        SoulbindingData.clear(main);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return null;
    }

    private SoulbindingRitual() {
    }
}
