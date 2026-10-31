package org.brahypno.maledict.common.item.curio;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import static org.brahypno.maledict.common.item.EffectTooltipComponents.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/** Vision abilities accumulate on top of stage zero's Omen. */
public final class EidolonOfTheHighPriestessItem extends AbstractVisionCurioItem {
    public static final String STAGES_TAG = "HighPriestessStages";
    public static final int MAX_STAGE = 4;
    private static final String WOUND_EFFECT = "maledict.high_priestess.wound_yet_to_come";

    public EidolonOfTheHighPriestessItem() {
        super(STAGES_TAG, MAX_STAGE);
    }

    public static int stage(ItemStack stack) {
        return stack.getItem() instanceof EidolonOfTheHighPriestessItem item ? item.getStage(stack) : 0;
    }

    public static boolean advance(ItemStack stack) {
        return stack.getItem() instanceof EidolonOfTheHighPriestessItem item && item.advanceStage(stack);
    }

    public static ItemStack create(int stage) {
        return ((EidolonOfTheHighPriestessItem) MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get()).createAtStage(stage);
    }

    public static boolean hasWoundYetToCome(LivingEntity entity) {
        return equippedStage(entity) >= 2;
    }

    public static int equippedStage(LivingEntity entity) {
        return ((EidolonOfTheHighPriestessItem) MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get()).getEquippedStage(entity);
    }

    @Override
    public void addAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> attributes,
                                      SlotContext context, ItemStack stack) {
        if (!isVisionSlot(context)) {
            return;
        }
        addAttributeModifier(attributes, MaledictAttributes.TRANSCEND_OMEN,
                new AttributeModifier(ResourceLocation.parse("maledict:high_priestess/transcend_omen"), 0.5D,
                        AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltip, TooltipContext context, ItemStack stack) {
        List<Component> lines = super.getAttributesTooltip(tooltip, context, stack);
        if (stage(stack) >= 1) {
            lines.add(positiveEffect("maledict.high_priestess.life_manifestation"));
        }
        if (stage(stack) >= 2) {
            lines.add(positiveEffect(WOUND_EFFECT));
        }
        if (stage(stack) >= 3) {
            lines.add(positiveEffect("maledict.high_priestess.soul_displacement"));
        }
        return lines;
    }
}
