package org.brahypno.maledict.mixin;

import com.sammy.malum.core.handlers.TouchOfDarknessHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.maledict.common.realm.WeepingWellTravel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Malum 1.6.7 has no cancellable pre-rejection hook. Effect/attack events run after its
 * rejection packet and state changes, so they cannot replace the complete rejection.
 * Only this method is intercepted; all contact accumulation and unqualified rejection remain Malum's.
 */
@Mixin(value = TouchOfDarknessHandler.class, remap = false)
public abstract class TouchOfDarknessHandlerMixin {
    @Inject(method = "reject", at = @At("HEAD"), cancellable = true, remap = false)
    private void maledict$traverseWell(LivingEntity entity, CallbackInfo callback) {
        if (entity instanceof ServerPlayer player && WeepingWellTravel.tryTraverse(player)) callback.cancel();
    }
}
