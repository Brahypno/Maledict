package org.brahypno.maledict.common.lumenfall;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictAbilities;
import org.brahypno.maledict.common.MaledictClock;
import org.brahypno.maledict.common.MaledictDamageTypes;
import org.brahypno.maledict.common.MaledictProbeDamage;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * 沉辉（Lumenfall）：Tetra 沉辉铋武器的第二项特效，规范见
 * {@code 沉辉铋_Tetra双特效_实现规范_v3.md}。
 *
 * <p>时序上是三步：
 * <ol>
 *   <li><b>记账</b>：{@code LivingDamageEvent} 的 {@link EventPriority#LOWEST} 收下护甲之后、写血量
 *       之前的那份量，记 20%。致命的一击也在自己死亡之前先记进来，所以 {@code LivingDeathEvent}
 *       读到的是算上最后一击的存量，不用再去补算。</li>
 *   <li><b>定时释放</b>：最后有效命中之后 80 tick，用 {@code maledict:lumenfall_release} 打一记
 *       普通 {@code hurt}；这一记自己不再积蓄。</li>
 *   <li><b>死亡散辉</b>：目标先死了就按死亡位置 3.5 格拉一个球，球内每个活体各吃一次同样的量。
 *       散辉打死别人的话，那个人自己账本上的存量会接着散 —— 连锁靠队列循环，不靠递归函数。</li>
 * </ol>
 *
 * <p>这份监听刻意放在 {@link EventPriority#LOWEST}：同一事件里更靠后的同类监听仍然可能改写伤害，
 * 这里拿到的已经是最接近最终扣血的那一版，但不是「绝对最终」；推迟生命池把量清零之后这里就不会
 * 再记账，这正是规范第 4.2 节要的效果。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LumenfallEvents {
    private static final LumenfallLedger LEDGER = new LumenfallLedger();
    private static final Deque<DeathSpread> DEATHS = new ArrayDeque<>();

    /** 记账：一次合格命中记 20%，顺便刷新 80 tick 倒计时。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        // 自家伤害（沉辉释放、反照）不得再产生新的存量，否则一次连锁就能自激。
        if (MaledictDamageTypes.isOwn(source)) {
            return;
        }
        // 首版只认近战：造成者与直接实体必须是同一名攻击者。手持武器放箭、扔药水、踩到火都不算。
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
            return;
        }
        if (attacker == victim) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!MaledictAbilities.hasLumenfall(weapon)) {
            return;
        }
        double baseAttack = MaledictAbilities.weaponAttackDamage(
                weapon, attacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        if (baseAttack <= 0.0D) {
            return;
        }
        LEDGER.record(victim.getUUID(), attacker.getUUID(), victim.level().dimension().location(),
                      MaledictClock.now(victim), event.getAmount(), baseAttack);
    }

    /**
     * 死亡快照：只排队，不结算。{@code receiveCanceled = true} 是为了看见已经被别人取消的死亡 ——
     * 有复活机制的实体不能仅凭「进过死亡事件」就散辉，真正的判定留到本 tick 末尾。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled()) {
            return;
        }
        for (LumenfallMark mark : LEDGER.forVictim(victim.getUUID())) {
            if (mark.isDeathQueued()) {
                continue;
            }
            float amount = mark.deathAmount();
            if (amount <= 0.0F) {
                continue;
            }
            // 已排进死亡队列：顺手从到期索引里摘掉，别让它在到期时被子白取一次。
            LEDGER.suspend(mark);
            DEATHS.add(new DeathSpread(mark, victim.level().dimension().location(),
                                       victim.position(), amount));
        }
    }

    /** 服务端 tick 末尾：先结算到期的定时释放（可能顺手打出新的死亡候选），再确认并扩散死亡队列。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        releaseDue(server);
        spreadDeaths(server);
    }

    /** 服务端停止：暂态账本与队列一起丢掉，不把玩家引用带进下一次启动。 */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LEDGER.clear();
        DEATHS.clear();
        LumenfallVisuals.clear();
    }

    private static void releaseDue(MinecraftServer server) {
        long now = MaledictClock.now(server);
        // 整桶取走之后再结算：hurt 会同步触发伤害／死亡事件，可能往账本里加新条目，
        // 不能一边遍历桶一边改它。
        for (LumenfallMark mark : LEDGER.pollDue(now)) {
            Located located = locate(server, mark);
            if (located == null) {
                // 目标卸载或已经被移除：账本跟着走，不为它加载区块。
                LEDGER.remove(mark);
                continue;
            }
            ServerLevel level = located.level();
            LivingEntity victim = located.victim();
            float amount = mark.beginRelease();
            if (amount <= 0.0F) {
                LEDGER.remove(mark);
                continue;
            }
            ServerPlayer owner = owner(server, mark);
            float dealt = MaledictProbeDamage.hurtOnly(
                    victim, MaledictDamageTypes.source(level, MaledictDamageTypes.LUMENFALL_RELEASE, owner), amount);
            LumenfallVisuals.release(victim);
            // 免伤、被无敌帧挡住或被事件取消（dealt == 0）也算尝试过了，不重试刷伤害；
            // 真打进去又没打死，就消费掉这份积蓄；打死了交给死亡事件那边的散辉队列。
            if (dealt <= 0.0F || !victim.isDeadOrDying()) {
                LEDGER.remove(mark);
            }
        }
    }

    private static void spreadDeaths(MinecraftServer server) {
        int budget = LumenfallMark.DEATH_BATCH;
        while (budget-- > 0 && !DEATHS.isEmpty()) {
            DeathSpread spread = DEATHS.poll();
            ServerLevel level = levelOf(server, spread.dimension());
            LivingEntity victim = level == null ? null : findEntity(level, spread.mark().victim());
            if (victim != null && victim.isAlive()) {
                // 死亡被取消或实体被复活：本次死亡候选作废，账本按原倒计时挂回索引继续积累。
                LEDGER.resume(spread.mark());
                continue;
            }
            LEDGER.remove(spread.mark());
            if (level == null) {
                continue;
            }
            ServerPlayer owner = owner(server, spread.mark());
            DamageSource source = MaledictDamageTypes.source(
                    level, MaledictDamageTypes.LUMENFALL_RELEASE, owner);
            List<LivingEntity> targets = new ArrayList<>();
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, spread.area())) {
                if (!candidate.isAlive() || candidate.getUUID().equals(spread.mark().victim())) {
                    continue;
                }
                // 判定用目标实体中心到死亡位置的距离，3.5 格内一律同等对待，不做距离衰减。
                if (candidate.getBoundingBox().getCenter().distanceToSqr(spread.position())
                            > LumenfallMark.SPREAD_RADIUS * LumenfallMark.SPREAD_RADIUS) {
                    continue;
                }
                targets.add(candidate);
            }
            for (LivingEntity target : targets) {
                // 同样是 D0 探针：每个目标只吃一记，真实扣掉多少由探针报告；散辉打死的目标
                // 会在自己的死亡事件里把属于它的那份账排进队列，连锁就这么接着走。
                MaledictProbeDamage.hurtOnly(target, source, spread.amount());
            }
            LumenfallVisuals.spread(level, spread.position(), targets);
        }
    }

    @Nullable
    private static ServerPlayer owner(MinecraftServer server, LumenfallMark mark) {
        // 在线玩家列表是唯一权威：玩家换维度、断线都不用额外记账，也不会为了归属去加载维度。
        return server.getPlayerList().getPlayer(mark.owner());
    }

    @Nullable
    private static ServerLevel levelOf(MinecraftServer server, ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    @Nullable
    private static LivingEntity findEntity(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid) instanceof LivingEntity living ? living : null;
    }

    /**
     * 按账本找到目标：先只查账本里记的那个维度（这就是预筛，平时就一次查询），
     * 找不到再比照其余已加载维度 —— 实体走过传送门、被跨维度传送时，账本记的维度会过期，
     * 以前这里会直接把那份积蓄丢掉，现在顺手把维度改正。
     *
     * <p>账本只存 UUID 不存实体引用，就是为了不把实体钉在内存里；代价是每次多一次
     * {@code ServerLevel#getEntity(UUID)}（实体表哈希查找），这在烂电脑上远比常驻实体划算。
     *
     * <p>维度不是 {@code expiry} 的键：{@link LumenfallMark#moveTo} 只影响这里的查找，
     * 不需要动到期索引。
     */
    @Nullable
    private static Located locate(MinecraftServer server, LumenfallMark mark) {
        ServerLevel recorded = levelOf(server, mark.dimension());
        if (recorded != null) {
            LivingEntity victim = findEntity(recorded, mark.victim());
            if (victim != null) {
                return victim.isAlive() ? new Located(recorded, victim) : null;
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level == recorded) {
                continue;
            }
            LivingEntity victim = findEntity(level, mark.victim());
            if (victim != null) {
                mark.moveTo(level.dimension().location());
                return victim.isAlive() ? new Located(level, victim) : null;
            }
        }
        return null;
    }

    private record Located(ServerLevel level, LivingEntity victim) {
    }

    private record DeathSpread(LumenfallMark mark, ResourceLocation dimension, Vec3 position, float amount) {
        AABB area() {
            return new AABB(position, position).inflate(LumenfallMark.SPREAD_RADIUS);
        }
    }

    private LumenfallEvents() {
    }
}
