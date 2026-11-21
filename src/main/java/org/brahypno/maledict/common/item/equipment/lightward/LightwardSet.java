package org.brahypno.maledict.common.item.equipment.lightward;

import com.sammy.malum.common.data.attachment.SoulWardData;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.brahypno.maledict.client.item.equipment.lightward.LightwardClientState;
import org.brahypno.maledict.common.MaledictTags;

import java.util.List;

/**
 * 扬弃逐光六件套：四件护甲 + 神侵恶刃 + 一把斧；只算穿戴 / 手持的件数。
 *
 * <p>1.21 侧的三处结构性差异：灵魂护盾从 1.20.1 的 capability 换成 Malum 1.8.2 的
 * {@link SoulWardData} 附件；属性名从 {@code AttributeRegistry.SOUL_WARD_CAP} 变成
 * {@code MalumAttributes.SOUL_WARD_CAPACITY}；附魔一律是 {@code Holder<Enchantment>}，
 * 名字用 {@code Enchantment#getFullname} 或 {@code description()} 拼。
 *
 * <p>4 级「未竟飞升」的游戏内等级不再逐件覆写 {@code Item#getEnchantmentLevel}：1.21 的正确入口是
 * {@code GetEnchantmentLevelEvent}（NeoForge 在 {@code ItemStack#getEnchantmentLevel} /
 * {@code getAllEnchantments} 里触发），由 {@code LightwardSetEvents} 统一抬，护甲、恶刃与两把斧
 * 走同一条路。本类只负责算出该抬多少（{@link #enchantBonus(ItemStack)}）与把它画进 tooltip。
 */
public final class LightwardSet {
    /**
     * 2 级：弃形，灵魂护盾尚存时 +15% 移速。
     */
    public static final int SPEED_THRESHOLD = 2;
    /**
     * 3 级：以魂代形，灵魂护盾尚存时追加抵消最终伤害。
     */
    public static final int ABNEGATION_THRESHOLD = 3;
    /**
     * 4 级：未竟飞升，附魔增幅按灵魂护盾充盈度取 0..3。
     */
    public static final int ENCHANT_THRESHOLD = 4;
    public static final int WEAPON_THRESHOLD = 5;
    public static final int FULL_SET = 6;

    /**
     * 充盈度分档：≤30% 给 +1，≤60% 给 +2，其余 +3；0% 一点都不给。
     */
    public static final double ASCENSION_LOW_FILL = 0.30D;
    public static final double ASCENSION_MID_FILL = 0.60D;
    public static final int ASCENSION_MAX_BONUS = 3;

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

    public static int enchantBonus(ItemStack stack) {
        if (!isSetPiece(stack)) {
            return 0;
        }
        LivingEntity owner = activeOwner(stack);
        return owner == null ? 0 : enchantBonus(owner, stack);
    }

    /**
     * 未竟飞升：充盈度 = 灵魂护盾实际值 / 容量。0% 不增幅，≤30% 给 +1，≤60% 给 +2，其余 +3。
     * 容量为 0 时视作充盈度 0。
     */
    public static int enchantBonus(LivingEntity owner, ItemStack stack) {
        if (owner == null || levelIfEquipped(owner, stack) < ENCHANT_THRESHOLD) {
            return 0;
        }
        double fill = soulWardFill(owner);
        if (fill <= 0.0D) {
            return 0;
        }
        if (fill <= ASCENSION_LOW_FILL) {
            return 1;
        }
        return fill <= ASCENSION_MID_FILL ? 2 : ASCENSION_MAX_BONUS;
    }

    /**
     * 灵魂护盾当前值，直接读 Malum 的玩家附件；非玩家或附件缺席恒为 0。
     */
    public static double soulWard(LivingEntity owner) {
        if (!(owner instanceof Player player)) {
            return 0.0D;
        }
        var type = MalumAttachmentTypes.SOUL_WARD.get();
        if (!player.hasData(type)) {
            return 0.0D;
        }
        SoulWardData data = player.getData(type);
        return data.getSoulWard();
    }

    /**
     * 灵魂护盾理论值，即 Malum 的灵魂护盾容量属性。
     */
    public static double soulWardCapacity(LivingEntity owner) {
        return owner == null ? 0.0D : owner.getAttributeValue(MalumAttributes.SOUL_WARD_CAPACITY);
    }

    /**
     * 实际值 / 理论值，夹在 0..1；容量非正时算 0。
     */
    public static double soulWardFill(LivingEntity owner) {
        double capacity = soulWardCapacity(owner);
        if (capacity <= 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, Math.max(0.0D, soulWard(owner) / capacity));
    }

    public static boolean hasSoulWard(LivingEntity owner) {
        return soulWard(owner) > 0.0D;
    }

    /**
     * 2 件套：灵魂护盾尚存时给移速。
     */
    public static boolean grantsSpeedBonus(LivingEntity owner) {
        return owner != null && equippedPieces(owner) >= SPEED_THRESHOLD && hasSoulWard(owner);
    }

    /**
     * 3 件套：灵魂护盾尚存时追加抵消最终伤害。
     */
    public static boolean grantsAbnegation(LivingEntity owner) {
        return owner != null && equippedPieces(owner) >= ABNEGATION_THRESHOLD && hasSoulWard(owner);
    }

    /**
     * 附魔读取入口没有持有者参数，只能在查询时反查持有者本身 —— 护盾值只能从他的数据里取。
     *
     * <p>服务端线程查在线玩家表；tooltip 走的是渲染线程，永远不是服务端线程，所以必须退回本地玩家
     * （1.20.1 的 {@code Dist.CLIENT} 分支）。少了这一支，客户端读到的增幅恒为 0，
     * 附魔行就永远是原始等级，也就永远拼不出「名 IV（II+II）」。
     */
    public static LivingEntity activeOwner(ItemStack stack) {
        if (!isSetPiece(stack)) {
            return null;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isEquipped(player, stack)) {
                    return player;
                }
            }
            return null;
        }
        return clientOwner(stack);
    }

    /**
     * 客户端只认本地玩家；判据放在这里，专用服务端走到这里就直接返回，不会去加载客户端类。
     */
    private static LivingEntity clientOwner(ItemStack stack) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }
        return LightwardClientState.activeOwnerFor(stack);
    }

    /**
     * 5 级「武器攻击更加真实」：主手是套装武器且件数够。这是整次挥砍<b>唯一</b>的门禁读数 ——
     * 轻档探针补满（{@code compensateDamage}）与认主生物的连带一刀（{@code IncursusBladeAttack#ownerOf}）
     * 共用它，护甲提示里也只有一行，所以别再给「套装在不在这条路上生效」加第二个判据。
     */
    public static boolean unlocksWeaponTopUp(LivingEntity owner, ItemStack stack) {
        if (owner == null || !isWeapon(stack) || owner.getMainHandItem() != stack) {
            return false;
        }
        return equippedPieces(owner) >= WEAPON_THRESHOLD;
    }

    /**
     * 摘掉原版写下的那几行附魔。
     *
     * <p>1.20.1 靠 {@code Item#getDefaultTooltipHideFlags} 交出 {@code TooltipPart.ENCHANTMENTS}
     * 的隐藏位，只藏附魔行；1.21.1 没有这种细粒度开关 —— {@code HIDE_ADDITIONAL_TOOLTIP} 只挡
     * {@code appendHoverText}，而 {@code ItemStack#getTooltipLines} 里的
     * {@code addToTooltip(DataComponents.ENCHANTMENTS)} 在它之外无条件执行。所以不摘的话，
     * 事件里再补一行就是同一个附魔两行，且与增幅是否触发无关。
     *
     * <p>摘法是按原始等级原样重算一遍：原版那一行就是 {@code Enchantment.getFullname(holder, 原始等级)}，
     * {@code MutableComponent} 有结构化的 {@code equals}，逐字段相同，能精确匹配（翻译缺失也一样）。
     * 返回后附魔行只由 {@link #appendEnchantments} 产出。
     */
    public static void stripVanillaEnchantments(ItemStack stack, List<Component> tooltip) {
        for (var entry : stack.getTagEnchantments().entrySet()) {
            int level = entry.getIntValue();
            if (level > 0) {
                tooltip.remove(Enchantment.getFullname(entry.getKey(), level));
            }
        }
    }

    /**
     * 原版附魔行已由 {@link #stripVanillaEnchantments} 摘掉，这里按存储顺序补回；
     * 4 级起显示「有效等级（真实等级+增幅）」。
     */
    public static void appendEnchantments(ItemStack stack, List<Component> tooltip) {
        ItemEnchantments raw = stack.getTagEnchantments();
        int bonus = enchantBonus(stack);
        for (Holder<Enchantment> enchantment : raw.keySet()) {
            int level = raw.getLevel(enchantment);
            if (level <= 0) {
                continue;
            }
            if (bonus <= 0) {
                tooltip.add(Enchantment.getFullname(enchantment, level));
                continue;
            }
            tooltip.add(Component.translatable(
                    ENCHANTMENT_TOOLTIP_KEY,
                    effectiveName(enchantment, level + bonus),
                    roman(level),
                    roman(bonus)).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 原版 enchantment.level.N 只翻到 10，超过十级会露出翻译键，所以自己拼名字。
     */
    private static Component effectiveName(Holder<Enchantment> holder, int level) {
        Enchantment enchantment = holder.value();
        MutableComponent name = enchantment.description().copy();
        name.withStyle(holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY);
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
