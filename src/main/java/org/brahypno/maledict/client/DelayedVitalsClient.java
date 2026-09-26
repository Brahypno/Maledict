package org.brahypno.maledict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.vitals.DelayedVitals;
import org.brahypno.maledict.common.vitals.DelayedVitalsPool;
import org.brahypno.maledict.network.DelayedVitalsPacket;
import org.jetbrains.annotations.Nullable;

/**
 * 延迟池的客户端那一半：收包时覆盖，其余时间用与服务端相同的公式自己往下推。
 *
 * <p>本地推演只是为了血条好看：两次入队之间服务端一个包都不发，而池子每 tick 都在缩小。
 * 客户端与服务端的 tick 不可能永远对齐，所以服务端每 {@code RESYNC_TICKS} 还会校正一次。
 *
 * <p>这里还要按住原版的一个误会：服务端每 tick 掉一点血就会每 tick 发一次血量包，而
 * {@code LocalPlayer#hurtTo} 把「血量同步值变低」一律当成挨打，顺手点亮受伤动画。见
 * {@link #suppressDrainHurtReaction}。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DelayedVitalsClient {

    /** 上一帧的血量，用来认出「这一下掉血是我们池子漏的」。 */
    private static float lastHealth = Float.NaN;

    /** 收到服务端的权威值，直接覆盖本地推演。 */
    public static void accept(DelayedVitalsPacket packet) {
        DelayedVitals vitals = localVitals();
        if (vitals != null) {
            vitals.set(packet.pendingDamage(), packet.pendingHeal());
        }
    }

    /** 每客户端 tick 推进一步；返回值是要落地的量，这里只关心池子变小。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastHealth = Float.NaN;
            return;
        }

        float health = player.getHealth();
        boolean dropped = !Float.isNaN(lastHealth) && health < lastHealth;
        DelayedVitals vitals = DelayedVitals.get(player);
        DelayedVitalsPool pool = vitals == null ? null : vitals.pool();

        if (dropped || (pool != null && !pool.isEmpty())) {
            suppressDrainHurtReaction(player);
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        lastHealth = health;

        if (pool == null || pool.isEmpty()) {
            return;
        }
        pool.releaseDamage();
        pool.releaseHeal();
    }

    /**
     * 让这一次血量同步被原版当成「只是血量变了」，而不是挨打。
     *
     * <p>原版 {@code LocalPlayer#hurtTo} 收到自己的血量同步时，只要比当前低就认成挨打：
     * {@code hurtDuration = 10; hurtTime = 10}（红覆盖 + 视角顿挫），并把 {@code invulnerableTime}
     * 刷成 20（血条闪烁的条件正是「血量变低且 invulnerableTime > 0」）。而我们的涓流每 tick 掉一点血、
     * 服务端每 tick 发一次血量包，于是每 tick 被认成挨打一次 —— 整个人在漏账期间一直挂在受伤状态里，
     * 不是十 tick。
     *
     * <p><b>必须走原版这个开关</b>（{@code flashOnSetHealth}：为 false 时 {@code hurtTo} 只更新血量。
     * 原版重生后第一次同步用的也是它），而不是事后把 {@code hurtTime} 按回 0：血量包的处理发生在
     * 客户端 tick 事件之后，我们按完 0，原版随后又点成 10，画面渲染用的是 10 —— 按不掉，只是日志里
     * 看着像好了。这个字段由 {@code META-INF/accesstransformer.cfg} 放开为 public。
     *
     * <p>开关会被 {@code hurtTo} 自己消费掉（走完 else 分支后置回 true），所以漏账期间每个 tick 都要
     * 重新按一次。服务端一行没改：音效、击退、无敌帧照旧由服务端的伤害管线处理。
     */
    private static void suppressDrainHurtReaction(LocalPlayer player) {
        player.flashOnSetHealth = false;
    }

    /** 血条读的就是这一份。 */
    @Nullable
    private static DelayedVitals localVitals() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : DelayedVitals.get(player);
    }

    private DelayedVitalsClient() {
    }
}
