package org.brahypno.maledict.common.item.weapon.axe;

import org.brahypno.maledict.common.item.MaledictItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.util.List;

/** One reward and one wound per completed probe, including its fallback damage. */
public final class LoverAxeItem extends AxeItem {
    public static final ResourceLocation COLOR_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lover_axe_color");
    public static final ResourceLocation WOUND_ID = ResourceLocation.parse("maledict:lover_axe_wound");
    private static final String COLOR_TAG = "LoverAxeLavender";

    public LoverAxeItem() {
        super(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(AxeItem.createAttributes(Tiers.NETHERITE, 5.0F, -3.0F)));
    }

    public static boolean isLavender(ItemStack stack) {
        return MaledictItemData.read(stack).getBoolean(COLOR_TAG);
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
        if (victim.isAlive()) {
            wound(victim);
        }
        if (fullyCharged) {
            AttributeInstance descent = attacker.getAttribute(MaledictAttributes.DESCENT);
            if (descent != null) {
                descent.setBaseValue(descent.getBaseValue() + 2.0D);
            }
        }
        if (attacker.getRandom().nextBoolean()) {
            MaledictItemData.update(weapon, tag -> tag.putBoolean(COLOR_TAG, !isLavender(weapon)));
        }
        return true;
    }

    private static void wound(LivingEntity victim) {
        AttributeInstance health = victim.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        AttributeModifier old = health.getModifier(WOUND_ID);
        double remaining = old == null ? 1.0D : 1.0D + old.amount();
        health.removeModifier(WOUND_ID);
        health.addPermanentModifier(new AttributeModifier(WOUND_ID, remaining * 0.98D - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (victim.getHealth() > victim.getMaxHealth()) {
            victim.setHealth(victim.getMaxHealth());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.maledict.lover_axe.wound").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maledict.lover_axe.descent").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.maledict.lover_axe.charged").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
