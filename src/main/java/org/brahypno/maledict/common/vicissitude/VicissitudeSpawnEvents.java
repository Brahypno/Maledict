package org.brahypno.maledict.common.vicissitude;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

/** Reject new boss summons in Peaceful while allowing existing world records to load or transfer. */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeSpawnEvents {
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof VicissitudeBossEntity boss
                && event.getLevel() instanceof ServerLevel level
                && level.getDifficulty() == Difficulty.PEACEFUL
                && !event.loadedFromDisk()
                && VicissitudeVitalityLedger.get(level).find(boss.getUUID()) == null) {
            event.setCanceled(true);
        }
    }

    private VicissitudeSpawnEvents() {}
}
