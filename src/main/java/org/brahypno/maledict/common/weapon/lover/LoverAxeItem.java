package org.brahypno.maledict.common.weapon.lover;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.util.List;
import java.util.UUID;

/** One reward and one wound per completed probe, including its fallback damage. */
public final class LoverAxeItem extends AxeItem {
    public static final UUID WOUND_ID = UUID.fromString("7fc0c676-3a3f-4886-980c-873c900b1826");
    private final boolean inflictsWounds;

    public LoverAxeItem() {
        this(true);
    }

    public LoverAxeItem(boolean inflictsWounds) {
        // Netherite contributes four damage and the player's base contributes one.
        super(Tiers.NETHERITE, inflictsWounds ? 6.0F : 3.0F, -3.0F,
                new Properties().rarity(Rarity.EPIC).fireResistant());
        this.inflictsWounds = inflictsWounds;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var attributes = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND) return attributes;
        return ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(attributes)
                .put(LodestoneAttributeRegistry.MAGIC_DAMAGE.get(), new AttributeModifier(
                        LodestoneAttributeRegistry.UUIDS.get(LodestoneAttributeRegistry.MAGIC_DAMAGE),
                        "Axe magic damage", inflictsWounds ? 1.0D : 2.0D,
                        AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (player.level().isClientSide()) {
            player.resetAttackStrengthTicker();
        } else {
            LoverAxeAttack.perform(player, entity);
        }
        return true;
    }

    /** One logical melee hit; the left-click handler resets attack strength after resolution. */
    public static boolean attack(Player attacker, Entity target, DamageSource source, float amount) {
        ItemStack weapon = attacker.getMainHandItem();
        if (attacker.level().isClientSide() || !(target instanceof LivingEntity victim)) {
            return target.hurt(source, amount);
        }
        if (victim == attacker || !victim.isAlive()) {
            return false;
        }
        boolean fullyCharged = attacker.getAttackStrengthScale(0.5F) >= 0.9F;
        var result = DamageProbe.mediumDamageMethod(target, source, amount);
        if (!result.instantEffective()) {
            return false;
        }
        if (victim.isAlive() && weapon.getItem() instanceof LoverAxeItem axe && axe.inflictsWounds) {
            wound(victim);
        }
        if (fullyCharged) {
            AttributeInstance descent = attacker.getAttribute(MaledictAttributes.DESCENT.get());
            if (descent != null) {
                descent.setBaseValue(descent.getBaseValue() + 2.0D);
            }
        }
        return true;
    }

    private static void wound(LivingEntity victim) {
        AttributeInstance health = victim.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        AttributeModifier old = health.getModifier(WOUND_ID);
        double remaining = old == null ? 1.0D : 1.0D + old.getAmount();
        health.removeModifier(WOUND_ID);
        health.addPermanentModifier(new AttributeModifier(WOUND_ID, "Lover's axe wound",
                remaining * 0.98D - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (victim.getHealth() > victim.getMaxHealth()) {
            victim.setHealth(victim.getMaxHealth());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (inflictsWounds) {
            tooltip.add(Component.translatable("tooltip.maledict.lover_axe.wound").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.maledict.lover_axe.descent").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.maledict.lover_axe.charged").withStyle(ChatFormatting.DARK_PURPLE));
        if (FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown()) {
            String name = inflictsWounds ? "lover_axe" : "meltfall_axe";
            tooltip.add(Component.translatable("tooltip.maledict." + name + ".flavor")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
