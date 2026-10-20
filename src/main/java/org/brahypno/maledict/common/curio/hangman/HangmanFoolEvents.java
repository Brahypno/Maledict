package org.brahypno.maledict.common.curio.hangman;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HangmanFoolEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLaunch(EntityJoinLevelEvent event) {
        // Move before tracking begins: the ordinary spawn packet already carries the new origin and motion.
        if (!event.getLevel().isClientSide() && !event.loadedFromDisk()
                && event.getEntity() instanceof Projectile projectile) {
            HangmanFoolRelay.redirect(projectile);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("removal")
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide){
            return;
        }
        if (event.getRayTraceResult() instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity wearer
            && projectile.getOwner() != wearer && HangmanFoolItem.equippedStage(wearer) >= 2
            && projectile.getDeltaMovement().lengthSqr() > 1.0E-12D
            && wearer.getRandom().nextDouble() < HangmanFoolProjectiles.reflectionChance(
                wearer.getHealth(), wearer.getMaxHealth(), MaledictConfig.HANGMAN_REFLECTION_BASE.get())){
            HangmanFoolProjectiles.reflect(projectile, wearer, hit.getLocation());
            event.setCanceled(true);
            MaledictNetwork.sendProjectileReflection(projectile);
            return;
        }
        if (event.getRayTraceResult() instanceof BlockHitResult hit
            && projectile.getOwner() instanceof LivingEntity shooter
            && HangmanFoolItem.equippedStage(shooter) >= 1
            && HangmanTrapEntity.capture(projectile, hit)){
            event.setCanceled(true);
            if (HangmanFoolItem.equippedStage(shooter) >= 3) {
                // Stage-three shots stop at obstacles after depositing their normal trap payload.
                projectile.discard();
            }
            //projectile.discard(); TODO：See if its good to discard or not
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (event.getItem().getUseAnimation() == UseAnim.BLOCK
            && HangmanFoolItem.equippedStage(event.getEntity()) >= 2){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (HangmanFoolItem.equippedStage(event.getEntity()) >= 2){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseTotem(LivingUseTotemEvent event) {
        if (HangmanFoolItem.equippedStage(event.getEntity()) >= 2){
            event.setCanceled(true);
        }
    }

    private HangmanFoolEvents() {
    }
}
