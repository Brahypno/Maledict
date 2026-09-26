package org.brahypno.maledict.common.vitals;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RuneOfMelancholiaItem;
import org.brahypno.maledict.network.MaledictNetwork;

/**
 * 抑郁符文的两半：{@code LivingHurtEvent} 上先适应（数值变钝），护甲之后在
 * {@code LivingDamageEvent} 上入队（时间变钝），池子由玩家 tick 结算。
 *
 * <p>顺序不能反：适应必须在入队之前，否则入队的量和实际掉的血对不上，减伤看起来像失效。
 *
 * <p>符文只是开关（入队要戴着），池子本身不绑符文 —— 摘下来、被没收都照常结算，只有死亡才清。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DelayedVitalsEvents {

    /** 低频校正间隔（tick）：客户端自己推演，服务端每 2 秒对一次表。 */
    public static final int RESYNC_TICKS = 40;

    /**
     * 适应：连着挨同一种伤害越来越钝。
     *
     * <p>挂 {@code LivingHurtEvent} 而不是 {@code LivingDamageEvent}，与仓库其它减伤逻辑一致；
     * 代价是这里的比例是护甲**前**的比例。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(victim instanceof Player player)) {
            return;
        }
        event.setAmount(event.getAmount() * RuneOfMelancholiaItem.adaptationMultiplier(player, event.getSource()));
    }

    /** 伤害入队：护甲、附魔、吸收都算完之后，入队的就是这一刻真要扣的血。 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        // 穿无敌的伤害（/kill、掉出世界）不排队，立即结算，判断与朽骨符文一致。
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (!(victim instanceof Player player) || !RuneOfMelancholiaItem.isEquipped(player)) {
            return;
        }
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }
        vitals.queueDamage(event.getAmount(), event.getSource());
        // 原版是在 setHealth 那一段里扣饱食度的，入队把那段跳过了，这里补上，免得白吃一份便宜。
        player.causeFoodExhaustion(event.getSource().getFoodExhaustion());
        event.setAmount(0.0F);
    }

    /** 治疗入队：满血时释放出来的那部分会被夹掉，不囤积。 */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(entity instanceof Player player) || !RuneOfMelancholiaItem.isEquipped(player)) {
            return;
        }
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }
        vitals.queueHeal(event.getAmount());
        event.setAmount(0.0F);
    }

    /** 每 tick 结算：先治疗、再伤害，让治疗有机会把死亡那一线拉回来。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }

        DelayedVitalsPool pool = vitals.pool();
        if (pool.isEmpty() && !vitals.isDirty()) {
            return;
        }

        if (player.getHealth() > 0.0F) {
            float heal = pool.releaseHeal();
            float damage = pool.releaseDamage();
            if (heal > 0.0F) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            }
            if (damage > 0.0F) {
                deliver(player, damage, vitals);
            }
            if (pool.isEmpty()) {
                vitals.forgetSource();
            }
        }

        sync(player, vitals);
    }

    /** 死亡清除：适应账目与两个池子一起清，并让客户端把血条上的待结算段收掉。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !(dead instanceof Player player)) {
            return;
        }
        RuneOfMelancholiaItem.clearAdaptation(player);
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }
        vitals.clear();
        if (player instanceof ServerPlayer serverPlayer) {
            MaledictNetwork.sendDelayedVitals(serverPlayer, 0.0F, 0.0F);
        }
    }

    /** 登录、重生、换维度都走这里：客户端本地推演从零开始，必须补一次权威值。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DelayedVitals vitals = DelayedVitals.get(player);
        if (vitals == null) {
            return;
        }
        MaledictNetwork.sendDelayedVitals(player, vitals.pool().pendingDamage(), vitals.pool().pendingHeal());
    }

    /**
     * 释放一记待扣除：不致命的直接写血量，只在这一下把血压到 0 时显式走死亡流程。
     *
     * <p><b>涓流绝不能走 {@code hurt}。</b>池子是按 tick 一点点放的，一旦写成「血量不够就走
     * {@code hurt}」，低血量时就会变成每隔十几 tick 给自己来一下：受伤动画一直闪、无敌帧被反复
     * 续上（连怪都打不动你了），而护甲又把每次那一点点削掉一截 —— 血条就卡在濒死线上、池子慢慢
     * 漏完，玩家全程「停在受伤状态」。
     *
     * <p>代价是延迟致死不吃原版的不死图腾（{@code checkTotemDeathProtection} 只在 {@code hurt}
     * 里调用）。这是有意的取舍：涓流必须安静，不能顺手给自己挂无敌帧。
     */
    private static void deliver(ServerPlayer player, float damage, DelayedVitals vitals) {
        float left = player.getHealth() - damage;
        if (left > 0.0F) {
            player.setHealth(left);
            return;
        }
        player.setHealth(0.0F);
        DamageSource source = vitals.earliestSource();
        player.die(source != null ? source : player.damageSources().generic());
    }

    private static void sync(ServerPlayer player, DelayedVitals vitals) {
        boolean due = player.level().getGameTime() % RESYNC_TICKS == 0;
        if (!vitals.isDirty() && !(due && !vitals.pool().isEmpty())) {
            return;
        }
        vitals.clearDirty();
        MaledictNetwork.sendDelayedVitals(player, vitals.pool().pendingDamage(), vitals.pool().pendingHeal());
    }

    private DelayedVitalsEvents() {
    }
}
