package org.brahypno.maledict.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.brahypno.maledict.common.effect.BlessedRegeneration;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「生灵之祝」的放大点：在 {@code FoodData#tick} 头尾量一次血量差，把自然回血那份按等级补上——
 * 没有任何事件能区分回血来源（生命恢复与饥饿回血都是一次 {@code heal(1)}）。只管服务端：
 * 客户端也有一份 {@code FoodData} 在跑，但血量由服务端说了算。
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    /** 本 tick 开始时的血量。{@code FoodData} 每个玩家一个实例，服务端逐 tick 串行处理，安全。 */
    @Unique
    private float maledict$healthBeforeTick;

    @Inject(method = "tick", at = @At("HEAD"))
    private void maledict$captureHealth(Player player, CallbackInfo callback) {
        maledict$healthBeforeTick = player.getHealth();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void maledict$blessNaturalRegeneration(Player player, CallbackInfo callback) {
        float healed = player.getHealth() - maledict$healthBeforeTick;
        if (healed <= 0.0F || player.level().isClientSide) {
            return;
        }

        MobEffectInstance blessing = player.getEffect(MaledictMobEffects.BLESSING_OF_LIFE.get());
        if (blessing == null) {
            return;
        }

        float extra = BlessedRegeneration.extraHeal(healed, blessing.getAmplifier());
        if (extra > 0.0F) {
            player.heal(extra);
        }
    }
}
