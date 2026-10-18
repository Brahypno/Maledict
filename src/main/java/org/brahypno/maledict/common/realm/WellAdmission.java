package org.brahypno.maledict.common.realm;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.common.rite.WisdomSacrifice;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class WellAdmission {
    public static boolean hasVisionEquipment(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inventory ->
                inventory.getStacksHandler(WisdomSacrifice.DELUSION_SLOT).map(handler -> {
                    var stacks = handler.getStacks();
                    for (int index = 0; index < stacks.getSlots(); index++) {
                        if (isVisionOnly(player, stacks.getStackInSlot(index))) return true;
                    }
                    return false;
                }).orElse(false)).orElse(false);
    }

    /** Inspect effective slot permissions, not item IDs, stages, occupied slots or render toggles. */
    public static boolean isVisionOnly(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        var slots = CuriosApi.getItemStackSlots(stack, player);
        var curio = CuriosApi.getCurio(stack).resolve();
        boolean vision = false;
        for (String slot : slots.keySet()) {
            if (curio.isPresent() && !curio.get().canEquip(new SlotContext(slot, player, 0, false, true))) continue;
            if (!WisdomSacrifice.DELUSION_SLOT.equals(slot)) return false;
            vision = true;
        }
        return vision;
    }

    private WellAdmission() {
    }
}
