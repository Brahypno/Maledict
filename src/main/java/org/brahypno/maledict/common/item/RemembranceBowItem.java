package org.brahypno.maledict.common.item;

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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.common.combat.RemembranceBowProjectileEffect;
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

        int charge = getUseDuration(bow) - remainingUseDuration;
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
        int charge = getUseDuration(bow) - timeLeft;
        charge = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(bow, level, player, charge, !ammunition.isEmpty() || hasInfiniteArrows);
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
            AbstractArrow arrow = customArrow(arrowItem.createArrow(level, ammunition, player));
            int reminiscenceLevel = EnchantmentHelper.getItemEnchantmentLevel(MaledictEnchantments.REMINISCENCE.get(), bow);
            RemembranceBowProjectileEffect.markForPhasing(arrow, 2.0D + reminiscenceLevel);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                                    power * 3.0F * ARROW_SPEED_MULTIPLIER, 1.0F);
            if (power == 1.0F){
                arrow.setCritArrow(true);
            }

            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
            if (powerLevel > 0){
                arrow.setBaseDamage(arrow.getBaseDamage() + powerLevel * 0.5D + 0.5D);
            }

            int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
            if (punchLevel > 0){
                arrow.setKnockback(punchLevel);
            }

            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0){
                arrow.setSecondsOnFire(100);
            }

            bow.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(player.getUsedItemHand()));
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
               || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
    }

    public int getDefaultProjectileRange() {return 20;}
}
