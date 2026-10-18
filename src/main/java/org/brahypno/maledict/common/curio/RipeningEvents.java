package org.brahypno.maledict.common.curio;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.RipeningBonus;
import org.brahypno.maledict.registry.MaledictMobEffects;

/**
 * 「熟成之赐」的经验加成挂钩：{@code MobEffect} 碰不到原版的经验发放，所以写在事件上。
 *
 * <p>只改这一次要发的量（{@code setAmount}）；发完再补一笔会再次触发本事件，自己乘自己。
 */
@EventBusSubscriber(modid = Maledict.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RipeningEvents {

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        MobEffectInstance ripening = player.getEffect(MaledictMobEffects.RIPENING);
        if (ripening == null) {
            return;
        }

        event.setAmount(RipeningBonus.ripened(
                event.getAmount(), ripening.getAmplifier(), player.getRandom().nextFloat()));
    }

    private RipeningEvents() {
    }
}
