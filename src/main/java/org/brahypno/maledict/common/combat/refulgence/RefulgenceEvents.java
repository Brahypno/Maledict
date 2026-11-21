package org.brahypno.maledict.common.combat.refulgence;

import com.sammy.malum.core.systems.events.SoulWardDamageEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictClock;
import org.brahypno.maledict.common.MaledictDamageTypes;
import org.brahypno.maledict.common.MaledictProbeDamage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 反照（Refulgence）：折辉铅铋共晶作为镶嵌时，把灵魂护盾实际减免掉的伤害的一部分存进当前
 * 那把武器，下一次有效攻击时当作独立魔法伤害打出去。
 *
 * <p>充能只认「已经执行的减免」，1.21 有两处入口：
 * <ol>
 *   <li>Malum 原生灵魂护盾。1.21 的 Malum 直接发 {@link SoulWardDamageEvent}，其中
 *       {@code getAbsorbedDamage()} 就是这一记护盾实际吃掉的量，不用再自己取差价，
 *       也不用像 1.20.1 那样为它挂 mixin。</li>
 *   <li>逐光扬弃的额外减免（Lightward 组）。它执行完减免后直接调
 *       {@link RefulgenceChargeService#charge}，同样不需要暂存确认。</li>
 * </ol>
 * 两处都只对「已经执行的减免」充能，不要求最后真的掉血。
 *
 * <p>释放分两步：攻击命中时只排队，服务端 tick 末尾才真的打出去，避开嵌套 {@code hurt} 的递归。
 * 那一击如果已经把人打死，充能不平白丢失 —— 留到下一次攻击。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class RefulgenceEvents {
    private static final List<PendingRelease> RELEASES = new ArrayList<>();

    /**
     * 灵魂护盾的充能点。事件到达时护盾已经扣掉，吸收量是最终值。
     */
    @SubscribeEvent
    public static void onSoulWardDamage(SoulWardDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAbsorbedDamage() <= 0.0D) {
            return;
        }
        RefulgenceChargeService.charge(player, event.getSource(), (float) event.getAbsorbedDamage());
    }

    /**
     * 下一次「真的成立」的武器攻击：只排队，不动 NBT、不打伤害。
     *
     * <p>反照自己这一发不再触发反照，否则一次命中能无限连发。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getNewDamage() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (MaledictDamageTypes.is(source, MaledictDamageTypes.REFULGENCE)) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) {
            return;
        }
        if (attacker == victim || !RefulgenceAbility.holds(attacker)) {
            return;
        }
        long tick = MaledictClock.now(victim);
        ItemStack weapon = attacker.getMainHandItem();
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

    /**
     * 服务端 tick 末尾统一派发：这时已经不在任何 hurt 调用栈里，可以安全地再打一记。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (RELEASES.isEmpty()) {
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
    }

    private static void dispatch(MinecraftServer server, PendingRelease pending) {
        ServerLevel level = server.getLevel(pending.dimension());
        if (level == null) {
            return;
        }
        if (!(level.getEntity(pending.victim()) instanceof LivingEntity victim) || !victim.isAlive()) {
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

    /**
     * 同一名攻击者在同一 tick 里可能命中好几个目标（横扫、多部位），一次攻击只放一记。
     */
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
