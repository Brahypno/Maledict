package org.brahypno.maledict.common.ritual.soulbinding;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 *     <li>the same convention for the creature's hurt sound, {@code <namespace>:entity.<path>.hurt}.</li>
 * </ol>
 *
 * <p>1.20.1 的第三级是 {@code LivingEntityHurtSoundAccess} mixin 直接叫 {@code getHurtSound(source)}；
 * 1.21 侧不加 mixin（见 AGENTS.md「mixin 是最后手段」），改成按注册表里的 {@code entity.<path>.hurt}
 * 约定查找——原版与绝大多数模组都照这个命名，能覆盖同一批生物。代价是「自定义 getHurtSound 覆盖
 * 但 id 不按约定」的生物会退化为静音。
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

    /**
     * The registered id of the sound the creature should lend, or {@code null} for silence.
     */
    @Nullable
    public static ResourceLocation resolve(LivingEntity entity, @Nullable DamageSource source) {
        ResourceLocation known = idOf(VANILLA_ATTACKS.get(entity.getType()));
        if (known != null){
            return known;
        }
        ResourceLocation conventional = convention(entity.getType(), "attack");
        if (conventional != null){
            return conventional;
        }
        return convention(entity.getType(), "hurt");
    }

    /**
     * {@code <namespace>:entity.<path>.<suffix>}, which is how Malum and most mods name attack and
     * hurt sounds. The vanilla table above already covers every vanilla creature the attack
     * convention would find, so in practice this step is the third party path.
     */
    @Nullable
    private static ResourceLocation convention(EntityType<?> type, String suffix) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null || typeId.getPath().isEmpty()){
            return null;
        }
        ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(
                typeId.getNamespace(), "entity." + typeId.getPath() + "." + suffix);
        return BuiltInRegistries.SOUND_EVENT.containsKey(candidate) ? candidate : null;
    }

    /**
     * Registry id of a sound, or {@code null} when it is absent or registered as silence.
     */
    @Nullable
    private static ResourceLocation idOf(@Nullable SoundEvent sound) {
        if (sound == null || sound == SoundEvents.EMPTY){
            // SoundEvents.EMPTY is registered under a real id but exists to mean "play nothing";
            // recording it would give the tool a sound that fires on every hit and is never heard.
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.getKey(sound);
    }

    private SoulbindingSoundResolver() {
    }
}
