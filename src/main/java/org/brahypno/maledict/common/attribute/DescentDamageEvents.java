package org.brahypno.maledict.common.attribute;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

/** Descent scales post-mitigation damage; attribute sources own its value and lifetime. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DescentDamageEvents {
    private DescentDamageEvents() {}

    // Run before NORMAL-priority consumers, including the delayed damage pool.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        float amount = event.getAmount();
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
        event.setAmount((float) Math.min(Float.MAX_VALUE, amount * multiplier));
    }

    private static double effectiveDescent(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(MaledictAttributes.DESCENT.get());
        if (attribute == null) {
            return 0.0D;
        }
        double value = attribute.getValue();
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
