package org.brahypno.maledict.mixin;

import com.sammy.malum.common.block.curiosities.weeping_well.VoidConduitBlockEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = VoidConduitBlockEntity.class, remap = false)
public abstract class VoidConduitBlockEntityMixin {
    /** Each output owns its NBT instead of sharing the recipe's template tag. */
    @ModifyArg(method = "spitOutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setTag(Lnet/minecraft/nbt/CompoundTag;)V", remap = true),
            index = 0, remap = false)
    private CompoundTag maledict$copyOutputTag(CompoundTag tag) {
        return tag == null ? null : tag.copy();
    }
}
