package org.brahypno.maledict.common.item.equipment.lightward;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.brahypno.maledict.Maledict;

import java.util.function.Consumer;

/**
 * 扬弃逐光护甲。
 *
 * <p>1.20.1 覆写的是 {@code getArmorTexture}、{@code getEnchantmentLevel}、{@code getAllEnchantments}、
 * {@code damageItem} 与 {@code getDefaultTooltipHideFlags}。贴图与耐久在 1.21 搬进了 NeoForge 的
 * {@link IItemExtension}（贴图还多了一个 {@code ArmorMaterial.Layer} 参数），照旧覆写；
 * 附魔增幅改走 {@code GetEnchantmentLevelEvent}（见 {@code LightwardSetEvents}），不在物品上覆写，
 * 也不再挂 {@code HIDE_ADDITIONAL_TOOLTIP} —— 那个组件只挡 {@code appendHoverText}，
 * 原版附魔行照样由 {@code ItemStack#getTooltipLines} 写出来，真正的处理在 {@link LightwardTooltips}。
 */
public final class LightwardArmorItem extends ArmorItem implements IItemExtension {
    /**
     * 单件耐久：13/15/16/11 × 37，与 1.20.1 的材质表一致。
     */
    private static final int DURABILITY_MULTIPLIER = 37;

    public LightwardArmorItem(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    public static int durabilityFor(Type type) {
        int base = switch (type) {
            case HELMET -> 13;
            case CHESTPLATE -> 15;
            case LEGGINGS -> 16;
            case BOOTS -> 11;
            // 1.20.5 起多了给狼铠用的 BODY；本套装没有这一件，落到这里就是调用方写错了。
            case BODY -> throw new IllegalArgumentException("Lightward armor has no BODY type");
        };
        return base * DURABILITY_MULTIPLIER;
    }

    /**
     * 四件各用各的贴图；返回 {@code null} 时 NeoForge 会退回材质 layer 的默认贴图。
     */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        String name = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> null;
        };
        return name == null ? null : ResourceLocation.fromNamespaceAndPath(
                Maledict.MODID, "textures/models/armor/aufheben_lightward_" + name + ".png");
    }

    /**
     * 单次损失上限 20 点，避免一次暴击把整件打光。
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return Math.min(amount, 20);
    }
}
