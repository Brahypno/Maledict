package org.brahypno.maledict.common.vicissitude;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Projectile positions still precede the collision when vanilla invokes the victim's hurt(). */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeHitLocation {
    private record Impact(UUID victim, long tick, Vec3 point) {}
    private static final Map<Entity, Impact> IMPACTS = new WeakHashMap<>();

    private VicissitudeHitLocation() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recordImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide
            && event.getRayTraceResult() instanceof EntityHitResult hit
            && hit.getEntity() instanceof VicissitudeBossEntity boss) {
            IMPACTS.put(event.getProjectile(), new Impact(boss.getUUID(),
                    boss.level().getGameTime(), hit.getLocation()));
        }
    }

    static @Nullable Vec3 point(Entity projectile, VicissitudeBossEntity boss) {
        Impact impact = IMPACTS.get(projectile);
        return impact != null && impact.tick() == boss.level().getGameTime()
               && impact.victim().equals(boss.getUUID()) ? impact.point() : null;
    }
}
