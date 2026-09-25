package org.brahypno.maledict.common.curio;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 佩戴启蒙之年时的常驻冷却加速：每个服务端 tick 把 {@code endTime} 往前挪一格（原版自己还会再减一格），
 * 所以之后才进入冷却的物品同样受益。
 *
 * <p>用反射而不是 mixin：{@code ItemCooldowns.CooldownInstance} 是包私有的，包外连这个类型都写不出来。
 */
public final class CooldownSpeed {

    /** MCP 名；生产环境由 {@link ObfuscationReflectionHelper} 翻译成 SRG 名。 */
    private static final String COOLDOWNS_FIELD = "cooldowns";
    private static final String END_TIME_FIELD = "endTime";

    private static Field cooldownsField;
    private static Field endTimeField;
    private static boolean reflectionUnavailable;

    /** 把身上每一条还在走的冷却加快到 {@code speed} 倍；小于等于 1 视为不加速。 */
    public static void apply(Player player, double speed) {
        if (reflectionUnavailable || speed <= 1.0D) {
            return;
        }
        Map<Item, ?> active = activeCooldowns(player);
        if (active == null || active.isEmpty()) {
            return;
        }
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

    /** 直接改 {@code endTime}：{@code addCooldown} 会重置 {@code startTime}，每 tick 调一次冷却永远走不完。 */
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
