package org.brahypno.maledict.mixin;

import com.sammy.malum.common.enchantment.scythe.ReboundEnchantment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rebound has no event at the throw itself. {@code PlayerInteractEvent.RightClickItem} fires before
 * {@code Item#use} and cannot tell whether the throw actually happens (another listener may cancel
 * it, another item may override {@code use}), nor can it see the exact damage and launch position
 * Malum computed. Injecting just before the original throw disables the weapon slot is the only
 * point where the primary boomerang already exists and the player's attributes still include the
 * scythe's own attack-damage modifier. The Incursus Blade's separate throw is handled directly in
 * {@code IncursusBladeEnchantments}.
 */
@Mixin(value = ReboundEnchantment.class, remap = false)
public abstract class ReboundEnchantmentMixin {
    @Inject(
            method = "throwScythe",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/common/item/curiosities/TemporarilyDisabledItem;"
                             + "disable(Lnet/minecraft/server/level/ServerPlayer;I)V",
                    shift = At.Shift.BEFORE),
            remap = false)
    private static void maledict$spawnDiplopiaEchoes(
            Level level, Player player, InteractionHand hand, ItemStack scythe,
            CallbackInfo callback) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer){
            DiplopiaThrows.afterReboundThrow(level, serverPlayer, hand, scythe);
        }
    }
}
