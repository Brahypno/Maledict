package org.brahypno.maledict.common.refulgence;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictClock;
import org.brahypno.maledict.common.MaledictDamageTypes;
import org.brahypno.maledict.common.MaledictProbeDamage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 反照（Refulgence）：折辉铅铋共晶作为 Tetra 镶嵌时，把灵魂护盾实际减免掉的伤害的一部分存进当前
 * 那把武器，下一次有效攻击时当作独立魔法伤害打出去。规范见
 * {@code refracted_eutectic_refulgence_impl.md}。
 *
 * <p>两处减免各记各的，绝不互相减：
 * <ol>
 *   <li>Malum 原生灵魂护盾在 {@code LivingHurtEvent} 里跑，由 {@code SoulWardHandlerMixin} 只观察
 *       Malum 自己那次 {@code setAmount} 造成的变化量，{@code MONITOR} 才认账。</li>
 *   <li>逐光扬弃的额外减免在 {@code LivingDamageEvent} 里跑，由它自己把 {@code absorbed} 交给
 *       {@link RefulgencePending}，同样等 {@code MONITOR}。</li>
 * </ol>
 * 两处都只对「已经执行的减免」充能，不要求最后真的掉血；事件被取消就丢弃，但记录一定取走，
 * 不留在表里过夜。
 *
 * <p>释放同样分两步：攻击命中时只排队，服务端 tick 末尾才真的打出去，避开嵌套 {@code hurt} 的递归。
 * 那一击如果已经把人打死，充能不平白丢失 —— 留到下一次攻击。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RefulgenceEvents {
    private static final List<PendingRelease> RELEASES = new ArrayList<>();

    /**
     * Malum 灵魂护盾的确认点。
     *
     * <p>{@code receiveCanceled = true} 是必须的：事件一旦被取消，默认注解的监听器整个被跳过，
     * 这条待确认记录就永远留在表里。这里照样进去把它取走，取消就丢掉。
     */
    @SubscribeEvent(priority = EventPriority.MONITOR, receiveCanceled = true)
    public static void onLivingHurtConfirmed(LivingHurtEvent event) {
        RefulgencePending.Absorbed absorbed = RefulgencePending.takeWard(event);
        if (absorbed != null && !event.isCanceled()) {
            // 护盾分流到 0 的合法攻击也要算：这里不看事件剩下多少。
            RefulgenceChargeService.charge(absorbed);
        }
    }

    /** 逐光扬弃额外减免的确认点；与上一处是同一原始攻击里两个独立阶段，可以叠加。 */
    @SubscribeEvent(priority = EventPriority.MONITOR, receiveCanceled = true)
    public static void onLivingDamageConfirmed(LivingDamageEvent event) {
        RefulgencePending.Absorbed absorbed = RefulgencePending.takeAbandonment(event);
        if (absorbed != null && !event.isCanceled()) {
            RefulgenceChargeService.charge(absorbed);
        }
    }

    /** 下一次「真的成立」的武器攻击：只排队，不动 NBT、不打伤害。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        // 反照自己这一发不再触发反照，否则一次命中能无限连发。
        if (MaledictDamageTypes.is(source, MaledictDamageTypes.REFULGENCE)) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) {
            return;
        }
        if (attacker == victim) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!RefulgenceAbility.holds(attacker)) {
            return;
        }
        long tick = MaledictClock.now(victim);
        RefulgenceCharge charge = RefulgenceStore.read(weapon);
        if (charge.isExpired(tick)) {
            RefulgenceStore.clear(weapon);
            return;
        }
        if (charge.isEmpty() || alreadyQueued(attacker.getUUID(), tick)) {
            return;
        }
        RELEASES.add(new PendingRelease(weapon, attacker.getUUID(), victim.getUUID(),
                                        victim.level().dimension(), charge.charge(), tick));
    }

    /** 服务端 tick 末尾统一派发：这时已经不在任何 hurt 调用栈里，可以安全地再打一记。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RELEASES.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<PendingRelease> batch = new ArrayList<>(RELEASES);
        RELEASES.clear();
        for (PendingRelease pending : batch) {
            dispatch(server, pending);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RELEASES.clear();
        RefulgencePending.clear();
    }

    private static void dispatch(MinecraftServer server, PendingRelease pending) {
        ServerLevel level = server.getLevel(pending.dimension());
        if (level == null) {
            return;
        }
        LivingEntity victim = level.getEntity(pending.victim()) instanceof LivingEntity living ? living : null;
        if (victim == null || !victim.isAlive()) {
            // 那一击已经把人打死了（或者目标没了）：储能不平白丢，留到下一次有效攻击。
            return;
        }
        ItemStack weapon = pending.weapon();
        RefulgenceCharge charge = RefulgenceStore.read(weapon);
        float amount = charge.take(pending.amount());
        RefulgenceStore.write(weapon, charge);
        if (amount <= 0.0F) {
            return;
        }
        ServerPlayer attacker = server.getPlayerList().getPlayer(pending.owner());
        // 同一 tick 的追加一击：探针会在这次调用里把目标的无敌帧隔离掉、之后原样还原，
        // 否则主伤害刚拉满的 i-frame 只会把这一发当重复命中吞掉；还原也意味着我们不会
        // 反过来延长目标的无敌时间。伤害本身仍只走 D0（原版 hurt），不改任何防御计算。
        MaledictProbeDamage.hurtOnlyIsolated(
                victim, MaledictDamageTypes.source(level, MaledictDamageTypes.REFULGENCE, attacker), amount);
        RefulgenceVisuals.release(victim);
    }

    /** 同一名攻击者在同一 tick 里可能命中好几个目标（横扫、多部位），一次攻击只放一记。 */
    private static boolean alreadyQueued(UUID owner, long tick) {
        for (PendingRelease pending : RELEASES) {
            if (pending.owner().equals(owner) && pending.tick() == tick) {
                return true;
            }
        }
        return false;
    }

    private record PendingRelease(ItemStack weapon, UUID owner, UUID victim, ResourceKey<Level> dimension,
                                  float amount, long tick) {
    }

    private RefulgenceEvents() {
    }
}
