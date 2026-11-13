package org.brahypno.maledict.compat.maid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.brahypno.maledict.common.soulbinding.SoulbindingData;
import org.brahypno.maledict.registry.MaledictItems;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The gift relation on the original sword and the permanent maid soul on the transformed sword.
 *
 * <p>The original sword keeps a small independent record so it is never mistaken for the transformed
 * item, and the transformed sword reuses the ordinary {@link SoulbindingData#ROOT} record for its
 * fixed attribute snapshot and tooltip. The separate item types, not an NBT flag, decide which path
 * is allowed.
 */
public final class MaidBondData {
    public static final String ROOT = "MaledictMaidGift";
    public static final int VERSION = 1;
    public static final ResourceLocation MAID_ENTITY =
            ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "maid");
    public static final ResourceLocation MAID_HURT_PLAYER_SOUND =
            ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "maid.ai.hurt_player");

    private static final String VERSION_KEY = "Version";
    private static final String GIVER_KEY = "GiverMaidUUID";
    private static final String OWNER_KEY = "OriginalOwnerUUID";
    private static final String NAME_KEY = "GiverCustomName";
    private static final String LEVEL_KEY = "GiftLevel";
    private static final String SOUND_PACK_KEY = "SoundPackId";
    private static final String MODEL_KEY = "ModelId";
    private static final String OWNER_NAME_KEY = "OriginalOwnerName";

    public record Gift(UUID giverMaid, UUID originalOwner, @Nullable String originalOwnerName,
                       @Nullable Component customName, int giftLevel,
                       @Nullable String soundPackId, @Nullable String modelId) {
    }

    public static boolean isOriginal(ItemStack stack) {
        return stack.is(MaledictItems.YEARNING_SWORD.get());
    }

    public static boolean isMaid(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return MAID_ENTITY.equals(id);
    }

    @Nullable
    public static Gift read(ItemStack stack) {
        CompoundTag root = root(stack);
        if (root == null || root.getInt(VERSION_KEY) != VERSION
            || !root.hasUUID(GIVER_KEY) || !root.hasUUID(OWNER_KEY)){
            return null;
        }
        Component name = null;
        if (root.contains(NAME_KEY, Tag.TAG_STRING)){
            try {
                name = Component.Serializer.fromJson(root.getString(NAME_KEY));
            }
            catch (RuntimeException ignored) {
                // A damaged display name must not invalidate the relation.
            }
        }
        return new Gift(root.getUUID(GIVER_KEY), root.getUUID(OWNER_KEY),
                        string(root, OWNER_NAME_KEY), name,
                        root.getInt(LEVEL_KEY), string(root, SOUND_PACK_KEY), string(root, MODEL_KEY));
    }

    public static void write(ItemStack stack, Gift gift) {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, VERSION);
        root.putUUID(GIVER_KEY, gift.giverMaid());
        root.putUUID(OWNER_KEY, gift.originalOwner());
        if (gift.originalOwnerName() != null)
            root.putString(OWNER_NAME_KEY, gift.originalOwnerName());
        if (gift.customName() != null)
            root.putString(NAME_KEY, Component.Serializer.toJson(gift.customName()));
        root.putInt(LEVEL_KEY, gift.giftLevel());
        if (gift.soundPackId() != null)
            root.putString(SOUND_PACK_KEY, gift.soundPackId());
        if (gift.modelId() != null)
            root.putString(MODEL_KEY, gift.modelId());
        stack.getOrCreateTag().put(ROOT, root);
    }

    public static boolean matches(ItemStack stack, Gift gift) {
        Gift current = read(stack);
        return current != null && current.giverMaid().equals(gift.giverMaid())
               && current.originalOwner().equals(gift.originalOwner());
    }

    public static boolean sameGift(ItemStack first, ItemStack second) {
        Gift a = read(first);
        Gift b = read(second);
        return a != null && b != null && a.giverMaid().equals(b.giverMaid())
               && a.originalOwner().equals(b.originalOwner());
    }

    public static ItemStack createGiftSword(
            UUID giver, UUID owner, @Nullable String ownerName,
            @Nullable Component name, int level,
            @Nullable String soundPackId, @Nullable String modelId) {
        ItemStack stack = new ItemStack(MaledictItems.YEARNING_SWORD.get());
        write(stack, new Gift(giver, owner, ownerName, name, level, soundPackId, modelId));
        return stack;
    }

    public static ItemStack createTransformedSword(ItemStack original, LivingEntity maid) {
        ItemStack stack = new ItemStack(MaledictItems.FORSAKEN_VOW.get());
        Gift gift = read(original);
        if (gift != null)
            write(stack, gift);
        SoulbindingData.bind(stack, maid, null);
        // The Forsaken Vow has its own TLM voice packet. Never let the generic spirit sound play
        // TLM's file-less sound event: that is the subtitle-without-audio path.
        SoulbindingData.clearSound(stack);
        // TLM names a maid through getName()/getTypeName() (name tag first, then the selected model
        // name), not necessarily through LivingEntity#getCustomName. bindingName() also avoids team
        // decoration and leaves a localized vanilla type name out of the frozen record.
        Component capturedName = SoulbindingData.bindingName(maid);
        if (capturedName == null && gift != null)
            capturedName = gift.customName();
        SoulbindingData.setCustomName(stack, capturedName);
        EnchantmentHelper.getEnchantments(original)
                         .forEach((enchantment, level) -> stack.enchant(enchantment, level));
        return stack;
    }

    private static String string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : null;
    }

    @Nullable
    private static CompoundTag root(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(ROOT, Tag.TAG_COMPOUND))
            return null;
        return stack.getTag().getCompound(ROOT);
    }

    private MaidBondData() {
    }
}
