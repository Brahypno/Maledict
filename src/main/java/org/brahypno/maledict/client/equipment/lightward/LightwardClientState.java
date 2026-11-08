package org.brahypno.maledict.client.equipment.lightward;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.common.equipment.lightward.LightwardSet;

/** 客户端侧的持有者查询。 */
public final class LightwardClientState {
    public static int activeLevelFor(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 0 : LightwardSet.levelIfEquipped(minecraft.player, stack);
    }

    public static LivingEntity activeOwnerFor(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        return LightwardSet.levelIfEquipped(minecraft.player, stack) > 0 ? minecraft.player : null;
    }

    public static boolean viewerHasSoulWard() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player != null
               && LightwardSet.hasSoulWard(minecraft.player);
    }

    public static double viewerSoulWardFill() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.player == null
               ? 0.0D
               : LightwardSet.soulWardFill(minecraft.player);
    }

    public static int viewerPieces() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.player == null
               ? 0
               : LightwardSet.equippedPieces(minecraft.player);
    }

    public static boolean viewerMainhandUnlocked() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player != null
               && LightwardSet.unlocksWeaponTopUp(minecraft.player, minecraft.player.getMainHandItem());
    }

    private LightwardClientState() {
    }
}
