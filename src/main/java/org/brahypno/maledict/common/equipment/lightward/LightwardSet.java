package org.brahypno.maledict.common.equipment.lightward;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.brahypno.maledict.client.equipment.lightward.LightwardClientState;
import org.brahypno.maledict.common.MaledictTags;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 扬弃逐光六件套：四件护甲 + 神侵恶刃 + 一把斧；只算穿戴 / 手持的件数。 */
public final class LightwardSet {
    public static final int ENCHANT_THRESHOLD = 4;
    public static final int WEAPON_THRESHOLD = 5;
    public static final int FULL_SET = 6;
    public static final int ENCHANT_BONUS = 3;

    public static final String ENCHANTMENT_TOOLTIP_KEY =
            "tooltip.maledict.aufheben_lightward.enchantment";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private LightwardSet() {
    }

    public static boolean isArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MaledictTags.AUFHEBEN_LIGHTWARD_ARMOR);
    }

    public static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MaledictTags.AUFHEBEN_LIGHTWARD_WEAPONS);
    }

    public static boolean isBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MaledictTags.AUFHEBEN_LIGHTWARD_BLADES);
    }

    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MaledictTags.AUFHEBEN_LIGHTWARD_AXES);
    }

    public static boolean isSetPiece(ItemStack stack) {
        return isArmor(stack) || isWeapon(stack);
    }

    public static int equippedPieces(LivingEntity owner) {
        if (owner == null) {
            return 0;
        }
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (isArmor(owner.getItemBySlot(slot))) {
                pieces++;
            }
        }
        if (isBlade(owner.getMainHandItem()) || isBlade(owner.getOffhandItem())) {
            pieces++;
        }
        if (isAxe(owner.getMainHandItem()) || isAxe(owner.getOffhandItem())) {
            pieces++;
        }
        return pieces;
    }

    public static int levelIfEquipped(LivingEntity owner, ItemStack stack) {
        if (owner == null || !isSetPiece(stack) || !isEquipped(owner, stack)) {
            return 0;
        }
        return equippedPieces(owner);
    }

    /** 附魔读取入口没有持有者参数，只能在查询时反查。 */
    public static int activeLevelFor(ItemStack stack) {
        if (!isSetPiece(stack)) {
            return 0;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            int best = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                best = Math.max(best, levelIfEquipped(player, stack));
            }
            return best;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return LightwardClientState.activeLevelFor(stack);
        }
        return 0;
    }

    public static int enchantBonus(ItemStack stack) {
        return isSetPiece(stack) && activeLevelFor(stack) >= ENCHANT_THRESHOLD ? ENCHANT_BONUS : 0;
    }

    public static int enchantBonus(LivingEntity owner, ItemStack stack) {
        return isSetPiece(stack) && levelIfEquipped(owner, stack) >= ENCHANT_THRESHOLD
               ? ENCHANT_BONUS
               : 0;
    }

    public static boolean unlocksWeaponTopUp(LivingEntity owner, ItemStack stack) {
        if (owner == null || !isWeapon(stack) || owner.getMainHandItem() != stack) {
            return false;
        }
        return equippedPieces(owner) >= WEAPON_THRESHOLD;
    }

    public static int boostedEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        int rawLevel = EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
        if (rawLevel <= 0) {
            return 0;
        }
        return rawLevel + enchantBonus(stack);
    }

    public static Map<Enchantment, Integer> boostedEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> raw =
                EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());
        int bonus = enchantBonus(stack);
        if (bonus <= 0) {
            return raw;
        }
        Map<Enchantment, Integer> boosted = new LinkedHashMap<>(raw.size());
        raw.forEach((enchantment, level) ->
                boosted.put(enchantment, level > 0 ? level + bonus : level));
        return boosted;
    }

    public static int viewerPieces() {
        return FMLEnvironment.dist == Dist.CLIENT ? LightwardClientState.viewerPieces() : 0;
    }

    public static boolean viewerMainhandUnlocked() {
        return FMLEnvironment.dist == Dist.CLIENT && LightwardClientState.viewerMainhandUnlocked();
    }

    /** 原版附魔行已被藏起，这里按 NBT 顺序补回；4 级起显示「有效等级（真实等级+增幅）」。 */
    public static void appendEnchantments(ItemStack stack, List<Component> tooltip) {
        if (!shouldAppendEnchantments(stack)) {
            return;
        }
        Map<Enchantment, Integer> raw =
                EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());
        int bonus = enchantBonus(stack);
        for (Map.Entry<Enchantment, Integer> entry : raw.entrySet()) {
            int level = entry.getValue();
            if (level <= 0) {
                continue;
            }
            Enchantment enchantment = entry.getKey();
            if (bonus <= 0) {
                tooltip.add(enchantment.getFullname(level));
                continue;
            }
            tooltip.add(Component.translatable(
                    ENCHANTMENT_TOOLTIP_KEY,
                    effectiveName(enchantment, level + bonus),
                    roman(level),
                    roman(bonus)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean shouldAppendEnchantments(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains("HideFlags", 99);
    }

    /** 原版 enchantment.level.N 只翻到 10，超过十级会露出翻译键。 */
    private static Component effectiveName(Enchantment enchantment, int level) {
        MutableComponent name = Component.translatable(enchantment.getDescriptionId());
        name.withStyle(enchantment.isCurse() ? ChatFormatting.RED : ChatFormatting.GRAY);
        if (level != 1 || enchantment.getMaxLevel() != 1) {
            name.append(Component.literal(" ")).append(Component.literal(roman(level)));
        }
        return name;
    }

    private static final int[] ROMAN_VALUES = {
            1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    };
    private static final String[] ROMAN_SYMBOLS = {
            "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    };

    private static String roman(int value) {
        if (value <= 0) {
            return "0";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (value >= ROMAN_VALUES[i]) {
                result.append(ROMAN_SYMBOLS[i]);
                value -= ROMAN_VALUES[i];
            }
        }
        return result.toString();
    }

    private static boolean isEquipped(LivingEntity owner, ItemStack stack) {
        if (isArmor(stack)) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                if (owner.getItemBySlot(slot) == stack) {
                    return true;
                }
            }
            return false;
        }
        if (isWeapon(stack)) {
            return owner.getMainHandItem() == stack || owner.getOffhandItem() == stack;
        }
        return false;
    }
}
