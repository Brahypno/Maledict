package org.brahypno.maledict.common.curio;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.RipeningBonus;
import org.brahypno.maledict.registry.MaledictMobEffects;

/**
 * 「熟成之赐」把经验抬高四分之一的那道挂钩。
 *
 * <h2>为什么挂在事件上而不是效果里</h2>
 * 原版的经验发放发生在 {@code Player#giveExperiencePoints} 里，{@code MobEffect} 碰不到它——
 * 效果没有「别人发经验」这种 tick。所以分工照旧：符文献效果、效果当标记，动手的写在这里，
 * 算术写在 {@link RipeningBonus}（纯数值，可单测）。
 *
 * <p>查的是<b>佩戴者身上有没有这个效果</b>，而不是有没有戴那枚符文：效果才是契约，
 * 于是命令、将来的更强来源、乃至别的模组给它挂上这个效果时，加成一样成立；
 * 效果每 40 tick 由符文刷一次，死了、跨维度、被 {@code /effect clear} 都最多 2 秒后自愈。
 *
 * <p>加成走 {@code XpChange#setAmount}——把这一次要发的量改掉，而不是发完再补一笔。
 * 补一笔会再次触发同一个事件，自己乘自己，几行代码就能滚成无限经验。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

        MobEffectInstance ripening = player.getEffect(MaledictMobEffects.RIPENING.get());
        if (ripening == null) {
            return;
        }

        event.setAmount(RipeningBonus.ripened(
                event.getAmount(), ripening.getAmplifier(), player.getRandom().nextFloat()));
    }

    private RipeningEvents() {
    }
}
