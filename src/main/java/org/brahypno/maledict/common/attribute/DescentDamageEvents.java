package org.brahypno.maledict.common.attribute;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

/** Descent scales post-mitigation damage; attribute sources own its value and lifetime. */
@EventBusSubscriber(modid = Maledict.MODID)
public final class DescentDamageEvents {
    private DescentDamageEvents() {}

    /**
     * 1.20.1 这一档是 HIGH，早于所有伤害消费者。NeoForge 的 {@code Pre} 在护甲之后，而同一个事件还要
     * 给「适应（乘法）→ 延迟生命池入队（LOW）→ 三个读取方（LOWEST）」排出固定台阶，所以这里取 NORMAL：
     *
     * <ul>
     *   <li>与 Lodestone 的魔法缩放同档，按模组加载顺序排在它之后（本模组依赖 Lodestone）；</li>
     *   <li>仍是乘法，与同档的适应倍率、Lodestone 的缩放交换，先后不影响结果；</li>
     *   <li>严格早于 LOW 的延迟池，所以入队的量带着 Descent 的倍率（1.20.1 的顺序语义）。</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        float amount = event.getNewDamage();
        if (victim.level().isClientSide() || !Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity)) {
            attacker = event.getSource().getDirectEntity();
        }
        double descent = effectiveDescent(victim);
        // An entity occupying both roles contributes only once.
        if (attacker instanceof LivingEntity living && living != victim) {
            descent += effectiveDescent(living);
        }
        double multiplier = 1.0D + 3.0D * (1.0D - Math.exp(-descent / 10.0D));
        event.setNewDamage((float) Math.min(Float.MAX_VALUE, amount * multiplier));
    }

    private static double effectiveDescent(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(MaledictAttributes.DESCENT);
        if (attribute == null) {
            return 0.0D;
        }
        double value = attribute.getValue();
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
