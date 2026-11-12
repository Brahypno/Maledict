package org.brahypno.maledict.common.soulbinding;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The versioned soulbinding record stored on the tool itself.
 *
 * <p>Layout follows the development specification's proposal:
 *
 * <pre>{@code
 * maledict_soulbinding: {
 *   Version: 1,
 *   State: "ARMED" | "BOUND",
 *   EntityType: "minecraft:enderman",
 *   CustomName: '{"text":"小黑"}',
 *   BaseAttributes: { "minecraft:generic.attack_damage": 7.0d, ... },
 *   SoundId: "minecraft:entity.enderman.hurt"
 * }
 * }</pre>
 *
 * <p>{@code SoundId} is optional and only ever a registered sound id: the spirit's attack sound is
 * resolved once, at sealing time, when the creature and its damage source still exist. A missing or
 * unresolvable id simply means the tool is silent, never a broken sound to play.
 *
 * <p>{@code EMPTY} is the absence of the record, not a stored value. The record never leaves the
 * {@link ItemStack}; no player state mirrors it.
 */
public final class SoulbindingData {
    /** Root compound key on the tool. */
    public static final String ROOT = "maledict_soulbinding";

    /** Current record version; unknown versions read as {@link State#EMPTY}. */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "Version";
    private static final String STATE_KEY = "State";
    private static final String ENTITY_TYPE_KEY = "EntityType";
    private static final String CUSTOM_NAME_KEY = "CustomName";
    private static final String BASE_ATTRIBUTES_KEY = "BaseAttributes";
    private static final String SOUND_ID_KEY = "SoundId";

    /**
     * Exactly the seven borrowed attributes, in tooltip order, with the first-version ratios and
     * stable modifier UUIDs. All modifiers are {@link AttributeModifier.Operation#ADDITION}.
     */
    public static final List<Bonus> BONUSES = List.of(
            new Bonus(Attributes.ATTACK_DAMAGE, 0.30D,
                    UUID.fromString("4a6d2f3e-1c7b-4e58-9a10-3f0b5c8d7e01")),
            new Bonus(Attributes.MAX_HEALTH, 0.04D,
                    UUID.fromString("5b7e3a4f-2d8c-4f69-8b21-4a1c6d9e8f02")),
            new Bonus(Attributes.ARMOR, 0.50D,
                    UUID.fromString("6c8f4b50-3e9d-4a7a-9c32-5b2d7e0f9a03")),
            new Bonus(Attributes.ARMOR_TOUGHNESS, 0.50D,
                    UUID.fromString("7d905c61-4fae-4b8b-8d43-6c3e8f1a0b04")),
            new Bonus(Attributes.KNOCKBACK_RESISTANCE, 0.40D,
                    UUID.fromString("8ea16d72-50bf-4c9c-9e54-7d4f9a2b1c05")),
            new Bonus(Attributes.MOVEMENT_SPEED, 0.05D,
                    UUID.fromString("9fb27e83-61c0-4dad-8f65-8e509b3c2d06")),
            new Bonus(Attributes.ATTACK_KNOCKBACK, 0.30D,
                    UUID.fromString("a0c38f94-72d1-4ebe-9a76-9f610c4d3e07"))
    );

    public enum State {
        EMPTY,
        ARMED,
        BOUND
    }

    /** One borrowed attribute and the fixed modifier identity used for it. */
    public record Bonus(Attribute attribute, double ratio, UUID uuid) {
        public double amount(double baseValue) {
            return baseValue * ratio;
        }
    }

    /** Decoded {@code BOUND} payload. Only values written by this mod are accepted. */
    public record Snapshot(@Nullable ResourceLocation entityType, @Nullable Component customName,
                           Map<Attribute, Double> baseAttributes,
                           @Nullable ResourceLocation soundId) {
        @Nullable
        public Double baseValue(Attribute attribute) {
            return baseAttributes.get(attribute);
        }
    }

    /** Reads the state, treating a missing or unreadable record as {@link State#EMPTY}. */
    public static State state(ItemStack stack) {
        CompoundTag root = root(stack);
        if (root == null || root.getInt(VERSION_KEY) != VERSION) {
            return State.EMPTY;
        }
        return switch (root.getString(STATE_KEY)) {
            case "ARMED" -> State.ARMED;
            case "BOUND" -> State.BOUND;
            default -> State.EMPTY;
        };
    }

    public static boolean isArmed(ItemStack stack) {
        return state(stack) == State.ARMED;
    }

    /** Arms an empty tool. The caller is responsible for every eligibility check. */
    public static void arm(ItemStack stack) {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, VERSION);
        root.putString(STATE_KEY, "ARMED");
        stack.getOrCreateTag().put(ROOT, root);
    }

    /** Drops the whole record; no spirit, seal or attribute data is refunded. */
    public static void clear(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        stack.getTag().remove(ROOT);
        if (stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
    }

    /**
     * Captures the entity's original base attributes and stores them on an already-armed stack.
     * Runtime modifiers, equipment, potions and temporary effects never enter the snapshot;
     * {@link AttributeInstance#getBaseValue()} is the only source.
     *
     * <p>The spirit's attack sound is resolved here, once, from the dying entity and the damage
     * source that killed it. The sound is only a registry id, so a tool that outlives a datapack
     * change stays readable; an absent id means the tool simply makes no extra sound.
     */
    public static boolean bind(ItemStack stack, LivingEntity entity, @Nullable DamageSource source) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (type == null) {
            return false;
        }

        Map<Attribute, Double> bases = new LinkedHashMap<>();
        for (Bonus bonus : BONUSES) {
            AttributeInstance instance = entity.getAttribute(bonus.attribute());
            if (instance == null) {
                continue;
            }
            double base = instance.getBaseValue();
            // A zero base contributes nothing. Omitting it keeps the record to the attributes that
            // actually matter for this kill.
            if (!Double.isFinite(base) || base == 0.0D) {
                continue;
            }
            bases.put(bonus.attribute(), base);
        }

        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, VERSION);
        root.putString(STATE_KEY, "BOUND");
        root.putString(ENTITY_TYPE_KEY, type.toString());

        Component customName = entity.getCustomName();
        if (entity.hasCustomName() && customName != null && !customName.getString().isEmpty()) {
            root.putString(CUSTOM_NAME_KEY, Component.Serializer.toJson(customName));
        }

        CompoundTag attributes = new CompoundTag();
        for (Map.Entry<Attribute, Double> entry : bases.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey());
            if (id != null) {
                attributes.putDouble(id.toString(), entry.getValue());
            }
        }
        root.put(BASE_ATTRIBUTES_KEY, attributes);
        ResourceLocation soundId = SoulbindingSoundResolver.resolve(entity, source);
        if (soundId != null) {
            root.putString(SOUND_ID_KEY, soundId.toString());
        }
        stack.getOrCreateTag().put(ROOT, root);
        return true;
    }

    /** Decoded payload for a {@code BOUND} stack, or {@code null} for any other state. */
    @Nullable
    public static Snapshot snapshot(ItemStack stack) {
        if (state(stack) != State.BOUND) {
            return null;
        }
        CompoundTag root = root(stack);
        if (root == null) {
            return null;
        }

        ResourceLocation type = root.contains(ENTITY_TYPE_KEY, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(root.getString(ENTITY_TYPE_KEY))
                : null;

        Component customName = null;
        if (root.contains(CUSTOM_NAME_KEY, Tag.TAG_STRING)) {
            try {
                Component parsed = Component.Serializer.fromJson(root.getString(CUSTOM_NAME_KEY));
                if (parsed != null && !parsed.getString().isEmpty()) {
                    customName = parsed;
                }
            } catch (Exception ignored) {
                // A malformed name is not a reason to lose the spirit; fall back to the type name.
            }
        }

        Map<Attribute, Double> attributes = new LinkedHashMap<>();
        if (root.contains(BASE_ATTRIBUTES_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag stored = root.getCompound(BASE_ATTRIBUTES_KEY);
            for (Bonus bonus : BONUSES) {
                ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(bonus.attribute());
                if (id == null || !stored.contains(id.toString(), Tag.TAG_DOUBLE)) {
                    continue;
                }
                double value = stored.getDouble(id.toString());
                if (Double.isFinite(value)) {
                    attributes.put(bonus.attribute(), value);
                }
            }
        }
        return new Snapshot(type, customName, Map.copyOf(attributes), soundId(root));
    }

    /**
     * The recorded sound id, if any. It is not checked against the registry here: a tool written by
     * an older or different mod set must stay readable, and playback skips an id that no longer
     * resolves instead of refusing the whole record.
     */
    @Nullable
    private static ResourceLocation soundId(CompoundTag root) {
        if (!root.contains(SOUND_ID_KEY, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(root.getString(SOUND_ID_KEY));
    }

    /**
     * The single spirit name shown in tooltips: a valid individual custom name first, then the
     * localized entity type, then the raw registry id.
     */
    public static Component displayName(Snapshot snapshot) {
        Component customName = snapshot.customName();
        if (customName != null && !customName.getString().isEmpty()) {
            return customName;
        }
        ResourceLocation type = snapshot.entityType();
        if (type != null) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
            if (entityType != null) {
                return entityType.getDescription();
            }
            return Component.literal(type.toString());
        }
        return Component.translatable("tooltip.maledict.soulbinding.unknown");
    }

    @Nullable
    private static CompoundTag root(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        return stack.getTag().getCompound(ROOT);
    }

    private SoulbindingData() {
    }
}
