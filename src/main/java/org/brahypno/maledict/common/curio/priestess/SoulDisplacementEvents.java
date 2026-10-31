package org.brahypno.maledict.common.curio.priestess;

import org.brahypno.maledict.common.entity.priestess.SoulEchoEntity;
import org.brahypno.maledict.common.item.curio.EidolonOfTheHighPriestessItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictEntities;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Maledict.MODID)
public final class SoulDisplacementEvents {
    private static final Map<UUID, SoulEchoEntity> ECHOES = new HashMap<>();

    // Before the shared delayed-vitals consumer sets the final amount to zero at LOW.
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || target instanceof SoulEchoEntity
                || event.getNewDamage() <= 0 || !event.getSource().is(LodestoneDamageTypeTags.IS_MAGIC)
                || !(event.getSource().getEntity() instanceof Player player)
                || player == target || !player.isAlive()
                || EidolonOfTheHighPriestessItem.equippedStage(player) < 3) return;

        SoulEchoEntity echo = ECHOES.get(target.getUUID());
        if (echo != null && echo.isForwarding()) return;
        if (echo == null || echo.isRemoved() || echo.level() != level) {
            if (echo != null) echo.discard();
            echo = new SoulEchoEntity(MaledictEntities.SOUL_ECHO.get(), level);
            echo.refresh(target);
            if (level.addFreshEntity(echo)) ECHOES.put(target.getUUID(), echo);
        } else {
            echo.refresh(target);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            SoulEchoEntity echo = ECHOES.get(event.getEntity().getUUID());
            if (echo != null) echo.discard();
        }
    }

    public static void forget(SoulEchoEntity echo) {
        ECHOES.remove(echo.targetUuid(), echo);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { ECHOES.clear(); }

    private SoulDisplacementEvents() {}
}
