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

    // Lodestone's NORMAL listener rebuilds magic damage from original damage;
    // run afterwards and before the LOWEST delayed pool consumes the remainder.
    @SubscribeEvent(priority = EventPriority.LOW)
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
