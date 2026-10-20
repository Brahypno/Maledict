package org.brahypno.maledict.common.item.weapon.bow;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.registry.MaledictEnchantments;

public final class RemembranceBowItem extends BowItem {
    private static final float DRAW_SPEED_MULTIPLIER = 2.0F;
    private static final float ARROW_SPEED_MULTIPLIER = 1.2F;
    private final boolean autoReleaseAtFullCharge;

    public RemembranceBowItem(Properties properties) {
        this(properties, false);
    }

    public RemembranceBowItem(Properties properties, boolean autoReleaseAtFullCharge) {
        super(properties);
        this.autoReleaseAtFullCharge = autoReleaseAtFullCharge;
    }

    public float getDrawSpeedMultiplier(ItemStack stack) {
        return DRAW_SPEED_MULTIPLIER;
    }

    public boolean shouldAutoReleaseAtFullCharge() {
        return autoReleaseAtFullCharge;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack bow, int remainingUseDuration) {
        if (!autoReleaseAtFullCharge || !(livingEntity instanceof Player player)) {
            return;
        }

        int charge = getUseDuration(bow, livingEntity) - remainingUseDuration;
        float power = getPowerForTime(Math.round(charge * getDrawSpeedMultiplier(bow)));
        if (power < 1.0F) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        player.releaseUsingItem();

        ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.isEmpty() && heldItem.getItem() == this && hasAmmunition(player, heldItem)) {
            player.startUsingItem(hand);
        }
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)){
            return;
        }

        boolean hasInfiniteArrows = hasInfiniteArrows(player, bow);
        ItemStack ammunition = player.getProjectile(bow);
        int charge = getUseDuration(bow, livingEntity) - timeLeft;
        charge = net.neoforged.neoforge.event.EventHooks.onArrowLoose(bow, level, player, charge, !ammunition.isEmpty() || hasInfiniteArrows);
        if (charge < 0 || ammunition.isEmpty() && !hasInfiniteArrows){
            return;
        }

        if (ammunition.isEmpty()){
            ammunition = new ItemStack(Items.ARROW);
        }

        float power = getPowerForTime(Math.round(charge * getDrawSpeedMultiplier(bow)));
        if (power < 0.1F){
            return;
        }

        boolean infiniteAmmunition = player.getAbilities().instabuild
                                     || ammunition.getItem() instanceof ArrowItem arrowItem
                                        && arrowItem.isInfinite(ammunition, bow, player);
        if (!level.isClientSide){
            ArrowItem arrowItem = ammunition.getItem() instanceof ArrowItem
                                  ? (ArrowItem) ammunition.getItem()
                                  : (ArrowItem) Items.ARROW;
            AbstractArrow arrow = customArrow(arrowItem.createArrow(level, ammunition, player, bow), ammunition, bow);
            RemembranceBowProjectileEffect.markForSoulExposure(arrow);
            int reminiscenceLevel = org.brahypno.maledict.registry.MaledictEnchantments.level(bow, MaledictEnchantments.REMINISCENCE);
            RemembranceBowProjectileEffect.markForPhasing(arrow, 2.0D + reminiscenceLevel);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                                    power * 3.0F * ARROW_SPEED_MULTIPLIER, 1.0F);
            if (power == 1.0F){
                arrow.setCritArrow(true);
            }

            // The arrow retains its firing weapon; vanilla now applies Power, Punch and Flame.
            bow.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (infiniteAmmunition || player.getAbilities().instabuild
                                      && (ammunition.is(Items.SPECTRAL_ARROW) || ammunition.is(Items.TIPPED_ARROW))){
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(arrow);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                        SoundSource.PLAYERS, 1.0F,
                        1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (!infiniteAmmunition && !player.getAbilities().instabuild){
            ammunition.shrink(1);
            if (ammunition.isEmpty()){
                player.getInventory().removeItem(ammunition);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static boolean hasAmmunition(Player player, ItemStack bow) {
        return hasInfiniteArrows(player, bow) || !player.getProjectile(bow).isEmpty();
    }

    private static boolean hasInfiniteArrows(Player player, ItemStack bow) {
        return player.getAbilities().instabuild
               || org.brahypno.maledict.registry.MaledictEnchantments.level(bow, Enchantments.INFINITY) > 0;
    }

    public int getDefaultProjectileRange() {return 20;}
}
