package org.brahypno.maledict.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.brahypno.maledict.client.curio.priestess.LifeManifestationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** See docs/design/high-priestess.md for the missing observer-specific Forge outline hook. */
@Mixin(Minecraft.class)
public abstract class MinecraftOutlineMixin {
    @Redirect(method = "shouldEntityAppearGlowing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;isCurrentlyGlowing()Z"))
    private boolean maledict$revealLife(Entity entity) {
        return entity.isCurrentlyGlowing() || LifeManifestationClient.reveals(entity);
    }
}
