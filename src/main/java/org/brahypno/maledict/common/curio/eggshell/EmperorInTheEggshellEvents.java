package org.brahypno.maledict.common.curio.eggshell;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;
import top.theillusivec4.curios.api.event.CurioUnequipEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 「蛋壳中的皇帝」的伤害窗口：同 tick 取最高，开窗批次全额，之后的命中按 {@code elapsed / N} 缩放。
 *
 * <p>为什么挂在 {@code LivingHurtEvent}：D0 普通 hurt 和 D1 直接 {@code actuallyHurt} 都会经过这里，
 * 所以它就是 D1 防御；D2+ 的 {@code setHealth} 不经过这个事件，天然穿透。不需要 mixin，也不需要
 * {@code CombatProtection.Guard}。
 *
 * <p>原版帧不碰：vanilla 的 20 tick 仍然在前，本窗口只处理穿过它、进入 {@code actuallyHurt} 的伤害。
 * 火/摔落等正常走原版受击的伤害会一样处理；带 {@code BYPASSES_INVULNERABILITY} 的来源（/kill、虚空等）
 * 直接放行。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EmperorInTheEggshellEvents {
    private static final Map<LivingEntity, EggshellWindow> WINDOWS = new WeakHashMap<>();

    /**
     * Scales the amount after the vanilla i-frame check, after any guard reduction and before armour.
     * Same-tick hits are one batch: only the largest raw amount is allowed to contribute.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!EmperorInTheEggshellItem.isEquipped(victim)) {
            WINDOWS.remove(victim);
            return;
        }
        DamageSource source = event.getSource();
        // 1.20.1: fire/fall and other sources that reach this event have already passed vanilla's
        // i-frame gate, so they are treated like any other hit. BYPASSES_INVULNERABILITY
        // (generic_kill, out_of_world, ...) is respected and left untouched.
        // BYPASSES_COOLDOWN is deliberately not treated as a bypass for this custom window.
        // TODO(1.21 / NeoForge): re-check DamageTypeTags and the LivingHurtEvent/LivingDamageEvent
        //  pipeline there; environmental damage, i-frame handling and source tags are not identical
        //  to Forge 1.20.1. Decide whether BYPASSES_COOLDOWN should skip the window on that line.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        int duration = MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get();
        float applied = WINDOWS.computeIfAbsent(victim, ignored -> new EggshellWindow())
                                .apply(victim.level().getGameTime(), amount, duration);
        if (!Float.isFinite(applied) || applied <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(applied);
    }

    /**
     * Starts the window when an opening hit lands. It deliberately does not require a positive
     * health loss: a hit fully eaten by armour or absorption still grants vanilla i-frames, so it
     * should start this window too. A later same-tick hit cannot refresh it; the window always runs
     * out from the batch's first landed hit.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!EmperorInTheEggshellItem.isEquipped(victim)) {
            WINDOWS.remove(victim);
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount < 0.0F) {
            return;
        }
        EggshellWindow window = WINDOWS.get(victim);
        if (window != null) {
            window.land(victim.level().getGameTime(), MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get());
        }
    }

    /** A removed charm should not keep a window alive for a re-equip within the same duration. */
    @SubscribeEvent
    public static void onCurioUnequip(CurioUnequipEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getStack().getItem() instanceof EmperorInTheEggshellItem) {
            WINDOWS.remove(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WINDOWS.clear();
    }

    private EmperorInTheEggshellEvents() {
    }
}
