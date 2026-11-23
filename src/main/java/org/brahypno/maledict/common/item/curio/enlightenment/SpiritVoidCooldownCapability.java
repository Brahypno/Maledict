package org.brahypno.maledict.common.item.curio.enlightenment;

import org.brahypno.maledict.registry.MaledictAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/**
 * 魂息虚空的内置冷却，挂在目标实体身上做限流；状态不落盘（重新载入时从 0 开始即可）。
 *
 * <p>挂在所有生物上，这样无论谁打中目标，查到的都是同一份记录。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class SpiritVoidCooldownCapability {

    /** 两次触发之间的最短间隔（tick）。 */
    public static final int COOLDOWN_TICKS = 100;

    /** 剩余冷却；0 表示可以触发。 */
    private int cooldown;

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        SpiritVoidCooldownCapability capability = get(livingEntity);
        if (capability != null) {
            capability.tickDown();
        }
    }

    /** 状态机本体：没在冷却就占一次机会并重新挂满冷却。 */
    boolean tryUse() {
        if (cooldown > 0) {
            return false;
        }
        cooldown = COOLDOWN_TICKS;
        return true;
    }

    /** 冷却逐 tick 减一，不会减成负数。 */
    void tickDown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /** 剩余冷却，测试与诊断用。 */
    int cooldown() {
        return cooldown;
    }

    /** 未挂载（或客户端侧缺同步）时返回 {@code null}。 */
    @Nullable
    public static SpiritVoidCooldownCapability get(LivingEntity entity) {
        return entity instanceof LivingEntity ? entity.getData(MaledictAttachments.SPIRIT_VOID_COOLDOWN) : null;
    }

    /**
     * 占用一次触发机会。
     *
     * <p>实体上没有这份 capability 时也返回 {@code true}：宁可刷一次，也不要让效果彻底失效。
     */
    public static boolean tryUse(LivingEntity target) {
        SpiritVoidCooldownCapability capability = get(target);
        return capability == null || capability.tryUse();
    }
}
