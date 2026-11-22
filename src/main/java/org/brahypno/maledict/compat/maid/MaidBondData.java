package org.brahypno.maledict.compat.maid;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.brahypno.maledict.common.ritual.soulbinding.SoulbindingData;
import org.brahypno.maledict.registry.MaledictItems;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The gift relation on the original sword and the permanent maid soul on the transformed sword.
 *
 * <p>The original sword keeps a small independent record so it is never mistaken for the transformed
 * item, and the transformed sword reuses the ordinary {@link SoulbindingData#ROOT} record for its
 * fixed attribute snapshot and tooltip. The separate item types, not an NBT flag, decide which path
 * is allowed.
 *
 * <p>本类**不引用任何女仆模组的类**：它只按注册名 {@code touhou_little_maid:maid} 判断实体类型，
 * 所以 {@code SoulbindingKill} 这类常驻监听器可以无条件地用它。真正会链接女仆模组的只有
 * {@code MaledictMaidIntegration} / {@code MaidEvents} / {@code MaledictMaidVoiceClient} 三个类。
 *
 * <p>1.21 与 1.20.1 的差异只在存储位置：物品根 NBT 已经不存在，{@code MaledictMaidGift} 这个子
 * compound 改存 {@link DataComponents#CUSTOM_DATA}（与 {@link SoulbindingData} 同一个组件、互不覆盖），
 * 读写统一走 {@link #root(ItemStack)} / {@link #writeRoot(ItemStack, Consumer)}。
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
                // 1.21 的反序列化要一个注册表视图；女仆名只是纯文本组件，EMPTY 足够，
                // 而且读一行名字不该依赖调用方手上有没有世界。
                name = Component.Serializer.fromJson(root.getString(NAME_KEY), RegistryAccess.EMPTY);
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
            root.putString(NAME_KEY, Component.Serializer.toJson(gift.customName(), RegistryAccess.EMPTY));
        root.putInt(LEVEL_KEY, gift.giftLevel());
        if (gift.soundPackId() != null)
            root.putString(SOUND_PACK_KEY, gift.soundPackId());
        if (gift.modelId() != null)
            root.putString(MODEL_KEY, gift.modelId());
        writeRoot(stack, tag -> tag.put(ROOT, root));
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
        // 1.21 的附魔是 Holder<Enchantment> 的 ItemEnchantments 组件，照 1.20.1 的做法整份带过去。
        // 读的是 {@code getTagEnchantments()}（组件里真正存着的那一份）：要复制的就是原剑上刻的附魔，
        // 物品自带或数据包追加的附魔不该被放大；{@code getEnchantments()} 在 1.21 已标废弃。
        ItemEnchantments enchantments = original.getTagEnchantments();
        for (var entry : enchantments.entrySet()) {
            stack.enchant(entry.getKey(), entry.getIntValue());
        }
        return stack;
    }

    private static String string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : null;
    }

    /**
     * The stored {@code MaledictMaidGift} compound, or {@code null} when absent. The returned tag is a
     * copy: edits belong in {@link #writeRoot(ItemStack, Consumer)}.
     */
    @Nullable
    private static CompoundTag root(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.isEmpty())
            return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT, Tag.TAG_COMPOUND))
            return null;
        return tag.getCompound(ROOT);
    }

    private static void writeRoot(ItemStack stack, Consumer<CompoundTag> mutator) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutator);
    }

    private MaidBondData() {
    }
}
