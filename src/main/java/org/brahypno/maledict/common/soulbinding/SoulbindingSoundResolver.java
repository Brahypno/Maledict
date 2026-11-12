package org.brahypno.maledict.common.soulbinding;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.maledict.mixin.LivingEntityHurtSoundAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Picks the attack sound a sealed creature lends to its new vessel.
 *
 * <p>The resolution order is fixed and must not be reversed into "hurt sound first, attack last":
 *
 * <ol>
 *     <li>the explicit vanilla table below, which also covers the sounds that do not end in
 *     {@code .attack} and the ones shared by two entities;</li>
 *     <li>the exact convention {@code <namespace>:entity.<path>.attack}, accepted only when that
 *     sound really is registered — no fuzzy search for {@code .hit}, {@code .roar} or
 *     {@code .shoot}, which would happily pick a sound the creature never makes when attacking;</li>
 *     <li>the creature's own {@code getHurtSound(DamageSource)}, fixed at sealing time because the
 *     damage source is part of the answer and the entity is gone afterwards.</li>
 * </ol>
 *
 * <p>Only vanilla entities are adapted here. Third party creatures go through the convention and
 * the hurt sound fallback; a pack can be covered later if a generic table ever turns out to be
 * needed.
 *
 * <p>Everything returns a registry id, never a {@link SoundEvent}: only the id is stored on the
 * tool, and a lookup that no longer resolves is skipped silently at playback.
 */
public final class SoulbindingSoundResolver {
    /**
     * Moves, calls and other sounds that are not named {@code .attack} but are what the creature
     * actually plays when it attacks. Two entries may share one sound: the Guardian and the Elder
     * Guardian both use {@code entity.guardian.attack}.
     */
    private static final Map<EntityType<?>, SoundEvent> VANILLA_ATTACKS = Map.ofEntries(
            Map.entry(EntityType.IRON_GOLEM, SoundEvents.IRON_GOLEM_ATTACK),
            Map.entry(EntityType.RAVAGER, SoundEvents.RAVAGER_ATTACK),
            Map.entry(EntityType.HOGLIN, SoundEvents.HOGLIN_ATTACK),
            Map.entry(EntityType.ZOGLIN, SoundEvents.ZOGLIN_ATTACK),
            Map.entry(EntityType.WARDEN, SoundEvents.WARDEN_ATTACK_IMPACT),
            Map.entry(EntityType.GUARDIAN, SoundEvents.GUARDIAN_ATTACK),
            Map.entry(EntityType.ELDER_GUARDIAN, SoundEvents.GUARDIAN_ATTACK),
            Map.entry(EntityType.BLAZE, SoundEvents.BLAZE_SHOOT),
            Map.entry(EntityType.GHAST, SoundEvents.GHAST_SHOOT),
            Map.entry(EntityType.WITHER, SoundEvents.WITHER_SHOOT),
            Map.entry(EntityType.EVOKER, SoundEvents.EVOKER_CAST_SPELL));

    /** The registered id of the sound the creature should lend, or {@code null} for silence. */
    @Nullable
    public static ResourceLocation resolve(LivingEntity entity, @Nullable DamageSource source) {
        ResourceLocation known = idOf(VANILLA_ATTACKS.get(entity.getType()));
        if (known != null) {
            return known;
        }
        ResourceLocation conventional = conventionalAttackSound(entity.getType());
        if (conventional != null) {
            return conventional;
        }
        return hurtSound(entity, source);
    }

    /**
     * {@code <namespace>:entity.<path>.attack}, which is how Malum and most mods name attack
     * sounds. The vanilla table above already covers every vanilla creature this convention would
     * find, so in practice this step is the third party path.
     */
    @Nullable
    private static ResourceLocation conventionalAttackSound(EntityType<?> type) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null || typeId.getPath().isEmpty()) {
            return null;
        }
        ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(
                typeId.getNamespace(), "entity." + typeId.getPath() + ".attack");
        return BuiltInRegistries.SOUND_EVENT.containsKey(candidate) ? candidate : null;
    }

    @Nullable
    private static ResourceLocation hurtSound(LivingEntity entity, @Nullable DamageSource source) {
        if (source == null || !(entity instanceof LivingEntityHurtSoundAccess access)) {
            return null;
        }
        return idOf(access.maledict$getHurtSound(source));
    }

    /** Registry id of a sound, or {@code null} when it is absent or registered as silence. */
    @Nullable
    private static ResourceLocation idOf(@Nullable SoundEvent sound) {
        if (sound == null || sound == SoundEvents.EMPTY) {
            // SoundEvents.EMPTY is registered under a real id but exists to mean "play nothing";
            // recording it would give the tool a sound that fires on every hit and is never heard.
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.getKey(sound);
    }

    private SoulbindingSoundResolver() {
    }
}
