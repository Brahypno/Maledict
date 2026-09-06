package org.brahypno.maledict.mixin;

import com.sammy.malum.registry.client.HiddenTagRegistry;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HiddenTagRegistry.class, remap = false) // malum是外部mod，禁用SRG remap
public abstract class HiddenTagRegistryMixin {
    //fixjeibug, shoot

    /**
     * Malum 在 JEI 插件构造时调用 HiddenTagRegistry.blankOutHidingTags()
     * runData 下没有 Minecraft 客户端实例，直接短路避免 NPE。
     */
    @Inject(method = "blankOutHidingTags", at = @At("HEAD"), cancellable = true)
    private static void dreamtinker$skipWhenNoClient(CallbackInfo ci) {
        if (Minecraft.getInstance() == null){
            ci.cancel(); // 直接返回，不执行原方法体
        }
    }
}

