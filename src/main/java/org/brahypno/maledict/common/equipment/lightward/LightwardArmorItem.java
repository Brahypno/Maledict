package org.brahypno.maledict.common.equipment.lightward;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.client.equipment.lightward.LightwardArmorClientExtensions;
import org.brahypno.maledict.config.MaledictConfig;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 扬弃逐光护甲；套装 tooltip 也写在这里。
 */
public final class LightwardArmorItem extends ArmorItem {
    private static final String SET_KEY = "tooltip.maledict.aufheben_lightward.set";
    private static final String COMPOSITION_KEY = "tooltip.maledict.aufheben_lightward.composition";
    private static final String COMPOSITION_ARMOR_KEY =
            "tooltip.maledict.aufheben_lightward.composition.armor";
    private static final String COMPOSITION_WEAPONS_KEY =
            "tooltip.maledict.aufheben_lightward.composition.weapons";
    private static final String EFFECT_2_KEY = "tooltip.maledict.aufheben_lightward.effect.2";
    private static final String EFFECT_3_KEY = "tooltip.maledict.aufheben_lightward.effect.3";
    private static final String EFFECT_4_KEY = "tooltip.maledict.aufheben_lightward.effect.4";
    private static final String EFFECT_5_KEY = "tooltip.maledict.aufheben_lightward.effect.5";

    /**
     * 已激活：亮蓝紫；未激活：暗灰。
     */
    private static final Style ACTIVE_STYLE = Style.EMPTY.withColor(0x9B7BFF);
    private static final Style INACTIVE_STYLE = Style.EMPTY.applyFormat(ChatFormatting.DARK_GRAY);

    /**
     * 灵魂护盾三项每件加成：容量 +7，恢复速率 +20%，稳固度 +10%。
     */
    private static final double SOUL_WARD_CAP_PER_PIECE = 7.0D;
    private static final double SOUL_WARD_RECOVERY_RATE_PER_PIECE = 0.2D;
    private static final double SOUL_WARD_INTEGRITY_PER_PIECE = 0.1D;

    private final UUID soulWardCapId;
    private final UUID soulWardRecoveryRateId;
    private final UUID soulWardIntegrityId;

    public LightwardArmorItem(Type type, Item.Properties properties) {
        super(LightwardArmorMaterial.INSTANCE, type, properties);
        this.soulWardCapId = soulWardModifierId(type, "cap");
        this.soulWardRecoveryRateId = soulWardModifierId(type, "recovery_rate");
        this.soulWardIntegrityId = soulWardModifierId(type, "integrity");
    }

    /**
     * 灵魂护盾三项不是 {@link net.minecraft.world.item.ArmorMaterial} 的字段，只能挂在物品上。
     * 同一属性上的修饰符按 UUID 去重，所以四件必须各用各的 UUID，否则后穿的那件会把先穿的顶掉。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != getType().getSlot()){
            return base;
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
        modifiers.putAll(base);
        modifiers.put(AttributeRegistry.SOUL_WARD_CAP.get(), new AttributeModifier(
                soulWardCapId, "Lightward soul ward capacity", SOUL_WARD_CAP_PER_PIECE,
                AttributeModifier.Operation.ADDITION));
        modifiers.put(AttributeRegistry.SOUL_WARD_RECOVERY_RATE.get(), new AttributeModifier(
                soulWardRecoveryRateId, "Lightward soul ward recovery", SOUL_WARD_RECOVERY_RATE_PER_PIECE,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        modifiers.put(AttributeRegistry.SOUL_WARD_INTEGRITY.get(), new AttributeModifier(
                soulWardIntegrityId, "Lightward soul ward integrity", SOUL_WARD_INTEGRITY_PER_PIECE,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        return modifiers.build();
    }

    private static UUID soulWardModifierId(Type type, String attribute) {
        return UUID.nameUUIDFromBytes(
                ("maledict:aufheben_lightward/" + type.getName() + "/soul_ward_" + attribute)
                        .getBytes(StandardCharsets.UTF_8));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new LightwardArmorClientExtensions());
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.HEAD){
            return "maledict:textures/models/armor/aufheben_lightward_helmet.png";
        }
        if (slot == EquipmentSlot.CHEST){
            return "maledict:textures/models/armor/aufheben_lightward_chestplate.png";
        }
        if (slot == EquipmentSlot.LEGS){
            return "maledict:textures/models/armor/aufheben_lightward_leggings.png";
        }
        if (slot == EquipmentSlot.FEET){
            return "maledict:textures/models/armor/aufheben_lightward_boots.png";
        }
        return super.getArmorTexture(stack, entity, slot, type);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return LightwardSet.boostedEnchantmentLevel(stack, enchantment);
    }

    @Override
    public Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        return LightwardSet.boostedEnchantments(stack);
    }

    @Override
    public int getDefaultTooltipHideFlags(ItemStack stack) {
        return ItemStack.TooltipPart.ENCHANTMENTS.getMask();
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        int pieces = LightwardSet.viewerPieces();
        tooltip.add(Component.translatable(SET_KEY, pieces)
                             .withStyle(ChatFormatting.DARK_PURPLE));
        if (FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown()){
            addSetDetails(tooltip, pieces);
        }
        LightwardSet.appendEnchantments(stack, tooltip);
    }

    private static void addSetDetails(List<Component> tooltip, int pieces) {
        tooltip.add(Component.translatable(COMPOSITION_KEY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(COMPOSITION_ARMOR_KEY)
                             .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(COMPOSITION_WEAPONS_KEY)
                             .withStyle(ChatFormatting.DARK_GRAY));
        boolean warded = LightwardSet.viewerHasSoulWard();
        addEffect(tooltip, EFFECT_2_KEY, pieces >= LightwardSet.SPEED_THRESHOLD && warded);
        addEffect(tooltip, EFFECT_3_KEY, pieces >= LightwardSet.ABNEGATION_THRESHOLD && warded,
                  abnegationPercent());
        addEffect(tooltip, EFFECT_4_KEY, pieces >= LightwardSet.ENCHANT_THRESHOLD
                                         && LightwardSet.viewerSoulWardFill() > 0.0D);
        addEffect(tooltip, EFFECT_5_KEY, LightwardSet.viewerMainhandUnlocked());
    }

    private static int abnegationPercent() {
        return (int) Math.round(MaledictConfig.LIGHTWARD_ABNEGATION_REDUCTION.get() * 100.0D);
    }

    private static void addEffect(List<Component> tooltip, String key, boolean active, Object... args) {
        tooltip.add(Component.translatable(key, args).withStyle(active ? ACTIVE_STYLE : INACTIVE_STYLE));
    }

    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return Math.min(amount, 20);
    }
}
