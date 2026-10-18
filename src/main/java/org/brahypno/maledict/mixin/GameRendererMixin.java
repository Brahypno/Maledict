package org.brahypno.maledict.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.brahypno.maledict.client.wisdom.WisdomBlackoutClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** 此时世界、手持物和后处理均已画完，HUD 尚未开始；也覆盖隐藏 HUD 的情况。 */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V", shift = At.Shift.AFTER))
    private void maledict$renderWisdomBlackout(DeltaTracker deltaTracker, boolean renderLevel,
                                              CallbackInfo callback) {
        WisdomBlackoutClient.render(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
