package org.brahypno.maledict.common.curio;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 佩戴启蒙之年时的常驻冷却加速：让玩家身上正在走的冷却每 tick 多走一格。
 *
 * <p>为什么是「每 tick 多走」而不是「装上时一次性砍短」：常驻效果必须覆盖**之后**才开始的
 * 冷却，一次性砍短只能照顾当下的那几条。所以这里每个服务端 tick 把 {@code endTime} 往前
 * 挪 {@code 速度 - 1} 格，原版自己每 tick 再减一格，合起来就是倍速。
 *
 * <p>为什么是反射而不是 mixin：冷却表 {@code ItemCooldowns.cooldowns} 是私有的，而表里的值
 * {@code ItemCooldowns.CooldownInstance} 是**包私有**的——包外的代码连这个类型都写不出来，
 * 所以 {@code @Mixin(ItemCooldowns.CooldownInstance.class)} 连编译都过不去。为一个字段加
 * 访问转换器又太重，于是用 Forge 自己提供的 {@link ObfuscationReflectionHelper}：它负责把
 * MCP 名按运行时映射（开发环境 MCP，生产环境 SRG）翻译过去。
 *
 * <p>已知代价：客户端那边只收到一次「剩余时长」，之后自己按原速倒数，所以冷却图标前半段会
 * 比实际慢一点、结束时被服务端拽回来。要消掉这一点得自己接管冷却同步（带着自己的数据结构
 * 再挂一个 capability），对一件饰品的常驻加速来说不划算。
 *
 * <p>反射失败不会炸游戏：记一次标记之后整条路径直接短路，宁可没有加速，也不要因为原版
 * 改了内部结构就每 tick 抛异常。
 */
public final class CooldownSpeed {

    /** MCP 名；生产环境由 {@link ObfuscationReflectionHelper} 翻译成 SRG 名。 */
    private static final String COOLDOWNS_FIELD = "cooldowns";
    private static final String END_TIME_FIELD = "endTime";

    private static Field cooldownsField;
    private static Field endTimeField;
    private static boolean reflectionUnavailable;

    /**
     * 把 {@code player} 身上每一条还在走的冷却加快到 {@code speed} 倍。
     *
     * <p>冷却表为空时直接返回，不做任何 Curios 查询——大多数时刻玩家身上并没有冷却。
     * 也因此这个方法的调用方可以放心地每 tick 调一次。
     *
     * @param speed 冷却速度倍率；{@code 2.0} 表示两倍速。小于等于 1 视为不加速。
     */
    public static void apply(Player player, double speed) {
        if (reflectionUnavailable || speed <= 1.0D) {
            return;
        }
        Map<Item, ?> active = activeCooldowns(player);
        if (active == null || active.isEmpty()) {
            return;
        }
        // 只有确实有冷却在走时，才值得去问 Curios 要一次查询。
        if (!AgeOfEnlightenmentItem.isEquipped(player)) {
            return;
        }

        // 每 tick 多挪 (speed - 1) 格：原版 tick 自己会再减 1 格。
        int bonus = (int) Math.round(speed) - 1;
        if (bonus <= 0) {
            return;
        }
        advance(active, bonus);
    }

    @SuppressWarnings("unchecked")
    private static Map<Item, ?> activeCooldowns(Player player) {
        try {
            if (cooldownsField == null) {
                cooldownsField = findField(ItemCooldowns.class, COOLDOWNS_FIELD);
            }
            return (Map<Item, ?>) cooldownsField.get(player.getCooldowns());
        } catch (Throwable throwable) {
            reflectionUnavailable = true;
            return null;
        }
    }

    /**
     * 把每条冷却的结束时间往前挪。
     *
     * <p>直接改 {@code endTime}，不走 {@code addCooldown}：那个方法会把 {@code startTime}
     * 一起重置，每 tick 调一次会让「已经过去的那一段」反复归零，冷却永远走不完。
     */
    private static void advance(Map<Item, ?> active, int bonus) {
        for (Object instance : active.values()) {
            if (instance == null) {
                continue;
            }
            try {
                if (endTimeField == null) {
                    endTimeField = findField(instance.getClass(), END_TIME_FIELD);
                }
                endTimeField.setInt(instance, endTimeField.getInt(instance) - bonus);
            } catch (Throwable throwable) {
                reflectionUnavailable = true;
                return;
            }
        }
    }

    private static Field findField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = ObfuscationReflectionHelper.findField(owner, name);
        field.setAccessible(true);
        return field;
    }

    private CooldownSpeed() {
    }
}
