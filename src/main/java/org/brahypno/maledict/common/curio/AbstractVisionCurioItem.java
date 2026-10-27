package org.brahypno.maledict.common.curio;

import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.brahypno.maledict.common.wisdom.WisdomSacrifice;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** Vision-only curios share cumulative stage markers; each item keeps its own NBT key. */
public abstract class AbstractVisionCurioItem extends MalumCurioItem {
    private final String stagesTag;
    private final int maxStage;

    protected AbstractVisionCurioItem(String stagesTag, int maxStage) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
        this.stagesTag = stagesTag;
        this.maxStage = maxStage;
    }

    public final int getStage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        CompoundTag stages = tag.getCompound(stagesTag);
        int stage = 0;
        while (stage < maxStage && stages.getBoolean("stage_" + (stage + 1))) {
            stage++;
        }
        return stage;
    }

    /** Adds one marker, retaining earlier abilities and all unrelated item data. */
    public final boolean advanceStage(ItemStack stack) {
        if (stack.getItem() != this) {
            return false;
        }
        int stage = getStage(stack);
        if (stage == maxStage) {
            return false;
        }
        CompoundTag stages = stack.getOrCreateTag().getCompound(stagesTag);
        stages.putBoolean("stage_" + (stage + 1), true);
        stack.getOrCreateTag().put(stagesTag, stages);
        return true;
    }

    protected final ItemStack createAtStage(int stage) {
        ItemStack stack = new ItemStack(this);
        for (int next = 1; next <= Math.min(maxStage, stage); next++) {
            advanceStage(stack);
        }
        return stack;
    }

    public final int getEquippedStage(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(inventory -> {
            var vision = inventory.getCurios().get(WisdomSacrifice.DELUSION_SLOT);
            int stage = -1;
            if (vision != null) {
                var stacks = vision.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() == this) {
                        stage = Math.max(stage, getStage(stack));
                    }
                }
            }
            return stage;
        }).orElse(-1);
    }

    protected static boolean isVisionSlot(SlotContext context) {
        return WisdomSacrifice.DELUSION_SLOT.equals(context.identifier());
    }

    @Override
    public final boolean canEquip(SlotContext context, ItemStack stack) {
        return isVisionSlot(context);
    }
}
