package org.brahypno.maledict.common.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

@EventBusSubscriber(modid = Maledict.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class LoverAxeEvents {
    private LoverAxeEvents() {}

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        LivingEntity entity = livingEntity;
        if (entity.level().isClientSide() || !entity.isAlive() || entity.tickCount % 20 != 0) return;
        var descent = entity.getAttribute(MaledictAttributes.DESCENT);
        if (descent == null) return;
        boolean holding = entity.getMainHandItem().getItem() instanceof LoverAxeItem
                || entity.getOffhandItem().getItem() instanceof LoverAxeItem;
        double previous = descent.getBaseValue();
        double next = Math.max(0.0D, previous + (holding ? 0.1D : -0.1D));
        if (next != previous) descent.setBaseValue(next);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        var health = event.getEntity().getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(LoverAxeItem.WOUND_ID);
        // End return also clones players; only an actual death clears the wound.
        if (!event.isWasDeath()) {
            var oldHealth = event.getOriginal().getAttribute(Attributes.MAX_HEALTH);
            var wound = oldHealth == null ? null : oldHealth.getModifier(LoverAxeItem.WOUND_ID);
            if (wound != null) health.addPermanentModifier(wound);
        }
    }
}
