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
 * 「生灵之祝」的放大点：在 {@code FoodData#tick} 的头尾量一次血量，把自然回血那份按等级补上。
 *
 * <h2>为什么挂在这里，而不是事件上</h2>
 * Forge 没有饱食度回血事件；{@code LivingHealEvent} 又分不清来源——生命恢复效果的
 * {@code heal(1)} 与饥饿值回血的 {@code heal(1)} 完全一样，靠事件猜必然误判。
 * {@code FoodData#tick} 里除了自然回血没有任何东西会动血量，所以头尾的血量差就是
 * 原版这一 tick 回了多少，误差为零。
 *
 * <h2>为什么这样最不容易和别的模组打架</h2>
 * <ul>
 *   <li>不 {@code @Redirect} 也不改原版 {@code heal} 的参数：原版那两次调用、力竭消耗、
 *       计时器全部保持原样，其他模组对同一处注入点做的 redirect / wrap 照常生效，
 *       不会因为抢同一个调用点而冲突崩游戏。</li>
 *   <li>两个注入点分别在最头与最尾，只读一次血量、只在真要补的时候才发一次 heal，
 *       每 tick 的开销就是一次 {@code getHealth()}。</li>
 *   <li>补发走 {@code Player#heal} 本身，所以 {@code LivingHealEvent} 照常触发，
 *       别的模组要拦、要记账都看得见这一份。</li>
 * </ul>
 *
 * <h2>只管服务端</h2>
 * 客户端也有一份 {@code FoodData} 在跑，但客户端的血量由服务端说了算。这里跳过客户端，
 * 免得本地先涨血再被服务端同步打回去。
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
