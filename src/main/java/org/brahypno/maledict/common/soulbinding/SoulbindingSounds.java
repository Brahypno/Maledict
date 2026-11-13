package org.brahypno.maledict.common.soulbinding;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plays the sealed spirit's attack sound when its vessel draws blood.
 *
 * <p>The sound is extra feedback on top of the vanilla hit and never replaces it: nothing here
 * touches the damage. It fires on {@link LivingHurtEvent}, server side only. That event is reached
 * only by damage that passed the target's immunity and invulnerability checks, and a listener that
 * cancels it earlier skips this handler entirely, so an air swing, a mining swing, a swap to another
 * item or a fully blocked hit cannot make a sound - unlike a listener on the client's swing
 * animation.
 *
 * <p>Only the main hand counts, matching the borrowed attributes. The sound is played at the
 * victim through {@code Level#playSound}, so every nearby player hears it, and the configured
 * cooldown keeps a single swing (melee hit, sweep and the arcane channel all arrive as separate
 * hurts) down to one sound.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingSounds {
    /** Quiet enough for a fast weapon; the vanilla hit keeps its own volume. */
    private static final float VOLUME = 0.75F;
    private static final float MIN_PITCH = 0.95F;
    private static final float MAX_PITCH = 1.05F;

    /** Last tick a spirit sound was played, per player; a cooldown, never damage. */
    private static final Map<UUID, Long> LAST_PLAYED = new HashMap<>();

    /**
     * {@link EventPriority#LOWEST} so a hit that another listener cancels earlier never reaches
     * this handler at all: cancelled events skip handlers that do not opt into them.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Arrows, thrown potions and summons carry their own direct entity; only the player's own
        // body (or a channel with no direct entity, like the arcane bonus) counts as its attack.
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != player) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        // The Forsaken Vow has a dedicated TLM voice packet. Its old/current record may still contain
        // a TLM sound id whose ogg only exists in the sound-pack system, which would be a subtitle
        // with no audio when played through Level#playSound.
        if (stack.is(MaledictItems.FORSAKEN_VOW.get())) return;
        SoulbindingData.Snapshot snapshot = SoulbindingData.snapshot(stack);
        if (snapshot == null) {
            return;
        }
        ResourceLocation soundId = snapshot.soundId();
        if (soundId == null) {
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(soundId).orElse(null);
        if (sound == null || sound == SoundEvents.EMPTY) {
            // The sound is gone after a datapack or mod change: stay silent, never re-resolve.
            return;
        }

        long now = player.level().getGameTime();
        Long last = LAST_PLAYED.get(player.getUUID());
        if (last != null && now - last < MaledictConfig.SOULBINDING_SPIRIT_SOUND_COOLDOWN.get()) {
            return;
        }
        LAST_PLAYED.put(player.getUUID(), now);

        player.level().playSound(null, target.getX(),
                                 target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                                 sound, SoundSource.PLAYERS, VOLUME,
                                 Mth.nextFloat(target.getRandom(), MIN_PITCH, MAX_PITCH));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_PLAYED.remove(event.getEntity().getUUID());
    }

    private SoulbindingSounds() {
    }
}
