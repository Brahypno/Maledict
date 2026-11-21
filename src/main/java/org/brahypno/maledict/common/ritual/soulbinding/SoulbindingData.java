package org.brahypno.maledict.common.ritual.soulbinding;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The versioned soulbinding record stored on the tool itself.
 *
 * <p>1.20.1 把记录写在 {@code ItemStack#getTag()} 上；1.21 没有物品根 NBT 了，记录改存
 * {@link DataComponents#CUSTOM_DATA}（{@link CustomData}）里的同一个 {@code maledict_soulbinding}
 * 子 compound，因此旧存档的值经组件反序列化后仍然可读，读写入口全部换成本类的
 * {@link #root(ItemStack)} / {@link #writeRoot(ItemStack, Consumer)}。
 *
 * <pre>{@code
 * maledict_soulbinding: {
 *   Version: 1,
 *   State: "ARMED" | "BOUND",
 *   EntityType: "minecraft:enderman",
 *   CustomName: '{"text":"小黑"}',
 *   BaseAttributes: { "minecraft:attack_damage": 7.0d, ... },
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
    /**
     * Root compound key on the tool.
     */
    public static final String ROOT = "maledict_soulbinding";

    /**
     * Current record version; unknown versions read as {@link State#EMPTY}.
     */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "Version";
    private static final String STATE_KEY = "State";
    private static final String ENTITY_TYPE_KEY = "EntityType";
    private static final String CUSTOM_NAME_KEY = "CustomName";
    private static final String BASE_ATTRIBUTES_KEY = "BaseAttributes";
    private static final String SOUND_ID_KEY = "SoundId";

    /**
     * Exactly the seven borrowed attributes, in tooltip order, with the first-version ratios and
     * stable modifier ids. All modifiers are {@link net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation#ADDITION}.
     * 1.21 的 {@code AttributeModifier} 用 {@code ResourceLocation} 认身份（UUID 已删），所以这里的
     * id 是对应 1.20.1 那七个 UUID 的固定字符串。
     */
    public static final List<Bonus> BONUSES = List.of(
            new Bonus(Attributes.ATTACK_DAMAGE, 0.30D, id("attack_damage")),
            new Bonus(Attributes.MAX_HEALTH, 0.04D, id("max_health")),
            new Bonus(Attributes.ARMOR, 0.50D, id("armor")),
            new Bonus(Attributes.ARMOR_TOUGHNESS, 0.50D, id("armor_toughness")),
            new Bonus(Attributes.KNOCKBACK_RESISTANCE, 0.40D, id("knockback_resistance")),
            new Bonus(Attributes.MOVEMENT_SPEED, 0.05D, id("movement_speed")),
            new Bonus(Attributes.ATTACK_KNOCKBACK, 0.30D, id("attack_knockback"))
    );

    public enum State {
        EMPTY,
        ARMED,
        BOUND
    }

    /**
     * One borrowed attribute and the fixed modifier identity used for it.
     */
    public record Bonus(Holder<Attribute> attribute, double ratio, ResourceLocation id) {
        public double amount(double baseValue) {
            return baseValue * ratio;
        }
    }

    /**
     * Decoded {@code BOUND} payload. Only values written by this mod are accepted.
     */
    public record Snapshot(@Nullable ResourceLocation entityType, @Nullable Component customName,
                           Map<Holder<Attribute>, Double> baseAttributes,
                           @Nullable ResourceLocation soundId) {
        @Nullable
        public Double baseValue(Holder<Attribute> attribute) {
            return baseAttributes.get(attribute);
        }
    }

    /**
     * Reads the state, treating a missing or unreadable record as {@link State#EMPTY}.
     */
    public static State state(ItemStack stack) {
        CompoundTag root = root(stack);
        if (root == null || root.getInt(VERSION_KEY) != VERSION){
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

    /**
     * Arms an empty tool. The caller is responsible for every eligibility check.
     */
    public static void arm(ItemStack stack) {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, VERSION);
        root.putString(STATE_KEY, "ARMED");
        writeRoot(stack, tag -> tag.put(ROOT, root));
    }

    /**
     * Drops the whole record; no spirit, seal or attribute data is refunded.
     */
    public static void clear(ItemStack stack) {
        if (root(stack) == null){
            return;
        }
        // The update helper removes the whole component once the tag becomes empty.
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(ROOT));
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
        if (type == null){
            return false;
        }

        Map<Holder<Attribute>, Double> bases = new LinkedHashMap<>();
        for (Bonus bonus : BONUSES) {
            AttributeInstance instance = entity.getAttribute(bonus.attribute());
            if (instance == null){
                continue;
            }
            double base = instance.getBaseValue();
            // A zero base contributes nothing. Omitting it keeps the record to the attributes that
            // actually matter for this kill.
            if (!Double.isFinite(base) || base == 0.0D){
                continue;
            }
            bases.put(bonus.attribute(), base);
        }

        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, VERSION);
        root.putString(STATE_KEY, "BOUND");
        root.putString(ENTITY_TYPE_KEY, type.toString());

        Component customName = bindingName(entity);
        if (customName != null){
            root.putString(CUSTOM_NAME_KEY, Component.Serializer.toJson(customName, entity.registryAccess()));
        }

        CompoundTag attributes = new CompoundTag();
        for (Map.Entry<Holder<Attribute>, Double> entry : bases.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().value());
            if (id != null){
                attributes.putDouble(id.toString(), entry.getValue());
            }
        }
        root.put(BASE_ATTRIBUTES_KEY, attributes);
        ResourceLocation soundId = SoulbindingSoundResolver.resolve(entity, source);
        if (soundId != null){
            root.putString(SOUND_ID_KEY, soundId.toString());
        }
        writeRoot(stack, tag -> tag.put(ROOT, root));
        return true;
    }

    /**
     * Decoded payload for a {@code BOUND} stack, or {@code null} for any other state.
     */
    @Nullable
    public static Snapshot snapshot(ItemStack stack) {
        if (state(stack) != State.BOUND){
            return null;
        }
        CompoundTag root = root(stack);
        if (root == null){
            return null;
        }

        ResourceLocation type = root.contains(ENTITY_TYPE_KEY, Tag.TAG_STRING)
                                ? ResourceLocation.tryParse(root.getString(ENTITY_TYPE_KEY))
                                : null;

        Component customName = null;
        if (root.contains(CUSTOM_NAME_KEY, Tag.TAG_STRING)){
            try {
                // The 1.21 serializer needs a lookup provider; the stack's own is the only one
                // available here, and a name never references dynamic registries in practice.
                Component parsed = Component.Serializer.fromJson(root.getString(CUSTOM_NAME_KEY),
                                                               net.minecraft.core.RegistryAccess.EMPTY);
                if (parsed != null && !parsed.getString().isEmpty()){
                    customName = parsed;
                }
            }
            catch (Exception ignored) {
                // A malformed name is not a reason to lose the spirit; fall back to the type name.
            }
        }

        Map<Holder<Attribute>, Double> attributes = new LinkedHashMap<>();
        if (root.contains(BASE_ATTRIBUTES_KEY, Tag.TAG_COMPOUND)){
            CompoundTag stored = root.getCompound(BASE_ATTRIBUTES_KEY);
            for (Bonus bonus : BONUSES) {
                ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(bonus.attribute().value());
                if (id == null || !stored.contains(id.toString(), Tag.TAG_DOUBLE)){
                    continue;
                }
                double value = stored.getDouble(id.toString());
                if (Double.isFinite(value)){
                    attributes.put(bonus.attribute(), value);
                }
            }
        }
        return new Snapshot(type, customName, Map.copyOf(attributes), soundId(root));
    }

    /**
     * Removes a recorded sound without touching the spirit, attributes or custom name.
     */
    public static void clearSound(ItemStack stack) {
        if (root(stack) == null)
            return;
        writeRoot(stack, tag -> tag.getCompound(ROOT).remove(SOUND_ID_KEY));
    }

    /**
     * Replaces the recorded custom name on an existing bound record.
     */
    public static void setCustomName(ItemStack stack, @Nullable Component customName) {
        if (root(stack) == null || !"BOUND".equals(root(stack).getString(STATE_KEY)))
            return;
        writeRoot(stack, tag -> {
            CompoundTag root = tag.getCompound(ROOT);
            if (customName == null || customName.getString().isEmpty()){
                root.remove(CUSTOM_NAME_KEY);
            }else {
                root.putString(CUSTOM_NAME_KEY, Component.Serializer.toJson(customName, net.minecraft.core.RegistryAccess.EMPTY));
            }
        });
    }

    /**
     * The name worth freezing into the record: a real custom name, or a non-vanilla name supplied by
     * a mod (TLM model names, for example). Plain {@link LivingEntity#getName()} avoids team colors
     * and prefixes from {@code getDisplayName()}, and an unchanged localized type description is
     * deliberately omitted so the tooltip keeps localizing itself on each client.
     */
    @Nullable
    public static Component bindingName(LivingEntity entity) {
        Component custom = entity.getCustomName();
        if (custom != null && !custom.getString().isEmpty())
            return custom;
        Component name = entity.getName();
        if (name == null || name.getString().isEmpty())
            return null;
        Component description = entity.getType().getDescription();
        if (description != null && name.getString().equals(description.getString()))
            return null;
        return name;
    }

    /**
     * The recorded sound id, if any. It is not checked against the registry here: a tool written by
     * an older or different mod set must stay readable, and playback skips an id that no longer
     * resolves instead of refusing the whole record.
     */
    @Nullable
    private static ResourceLocation soundId(CompoundTag root) {
        if (!root.contains(SOUND_ID_KEY, Tag.TAG_STRING)){
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
        if (customName != null && !customName.getString().isEmpty()){
            return customName;
        }
        ResourceLocation type = snapshot.entityType();
        if (type != null){
            var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
            if (entityType != null){
                return entityType.getDescription();
            }
            return Component.literal(type.toString());
        }
        return Component.translatable("tooltip.maledict.soulbinding.unknown");
    }

    /**
     * The stored {@code maledict_soulbinding} compound, or {@code null} when absent. The returned tag
     * is a copy: edits belong in {@link #writeRoot(ItemStack, Consumer)}.
     */
    @Nullable
    private static CompoundTag root(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.isEmpty()){
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT, Tag.TAG_COMPOUND)){
            return null;
        }
        return tag.getCompound(ROOT);
    }

    /**
     * Mutates the {@code maledict_soulbinding} compound in place and writes the component back.
     */
    private static void writeRoot(ItemStack stack, Consumer<CompoundTag> mutator) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> mutator.accept(tag));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soulbinding/" + path);
    }

    private SoulbindingData() {
    }
}
