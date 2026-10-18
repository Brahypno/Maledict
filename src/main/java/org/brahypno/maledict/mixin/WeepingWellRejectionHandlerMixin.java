package org.brahypno.maledict.mixin;

import com.sammy.malum.core.handlers.WeepingWellRejectionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.maledict.common.realm.WeepingWellTravel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Malum 1.8.2 has no cancellable pre-rejection hook. Effect/attack events run after its
 * rejection packet and state changes, so they cannot replace the complete rejection.
 * Only this method is intercepted; all contact accumulation and unqualified rejection remain Malum's.
 */
@Mixin(value = WeepingWellRejectionHandler.class, remap = false)
public abstract class WeepingWellRejectionHandlerMixin {
    @Inject(method = "launchPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void maledict$traverseWell(net.minecraft.world.entity.player.Player entity, CallbackInfo callback) {
        if (entity instanceof ServerPlayer player && WeepingWellTravel.tryTraverse(player)) callback.cancel();
    }
}
