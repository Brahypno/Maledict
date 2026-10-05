package org.brahypno.maledict.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stored shoulder chicks still grow; crouching releases only ravens using vanilla respawn. */
@Mixin(Player.class)
public abstract class PlayerRavenShoulderMixin {
    @Shadow
    private long timeEntitySatOnShoulder;

    @Shadow
    protected abstract void setShoulderEntityLeft(CompoundTag bird);

    @Shadow
    protected abstract void setShoulderEntityRight(CompoundTag bird);

    @Invoker("respawnEntityOnShoulder")
    protected abstract void maledict$respawnShoulderBird(CompoundTag bird);

    @Inject(method = "tick", at = @At("TAIL"))
    private void maledict$tickRavenShoulders(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide) {
            return;
        }
        maledict$tickRavenShoulder(player, player.getShoulderEntityLeft(), true);
        maledict$tickRavenShoulder(player, player.getShoulderEntityRight(), false);
    }

    @Unique
    private void maledict$tickRavenShoulder(Player player, CompoundTag stored, boolean left) {
        if (!"maledict:raven".equals(stored.getString("id"))) {
            return;
        }
        if (player.isShiftKeyDown() && timeEntitySatOnShoulder + 20L < player.level().getGameTime()) {
            maledict$respawnShoulderBird(stored);
            maledict$setRavenShoulder(new CompoundTag(), left);
        } else if (player.tickCount % 20 == 0 && stored.getInt("Age") != 0) {
            CompoundTag updated = stored.copy();
            int age = updated.getInt("Age");
            updated.putInt("Age", age < 0 ? Math.min(0, age + 20) : Math.max(0, age - 20));
            maledict$setRavenShoulder(updated, left);
        }
    }

    @Unique
    private void maledict$setRavenShoulder(CompoundTag bird, boolean left) {
        if (left) {
            setShoulderEntityLeft(bird);
        } else {
            setShoulderEntityRight(bird);
        }
    }
}
