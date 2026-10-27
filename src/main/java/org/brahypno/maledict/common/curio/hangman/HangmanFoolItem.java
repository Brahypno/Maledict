package org.brahypno.maledict.common.curio.hangman;

import com.google.common.collect.Multimap;
import com.sammy.malum.compability.irons_spellbooks.IronsSpellsCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.AbstractVisionCurioItem;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/** One vision curio, with cumulative per-stack stage markers. Stage four reserves no ability. */
public final class HangmanFoolItem extends AbstractVisionCurioItem {
    private static final String EFFECT_PREFIX = "maledict.hangman_fool.";
    private static final String FLAVOR_TEXT_PREFIX = "tooltip.maledict.hangman_fool.flavor.";
    public static final String STAGES_TAG = "HangmanFoolStages";
    public static final int MAX_STAGE = 4;
    public static final ResourceLocation STAGE_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "hangman_fool_stage");

    public HangmanFoolItem() {
        super(STAGES_TAG, MAX_STAGE);
    }

    public static int stage(ItemStack stack) {
        return stack.getItem() instanceof HangmanFoolItem item ? item.getStage(stack) : 0;
    }

    /** Adds the next marker without replacing names, enchantments, or other item NBT. */
    public static boolean advance(ItemStack stack) {
        return stack.getItem() instanceof HangmanFoolItem item && item.advanceStage(stack);
    }

    public static ItemStack create(int stage) {
        return ((HangmanFoolItem) MaledictItems.HANGMAN_FOOL.get()).createAtStage(stage);
    }

    public static int equippedStage(LivingEntity entity) {
        return ((HangmanFoolItem) MaledictItems.HANGMAN_FOOL.get()).getEquippedStage(entity);
    }

    @Override
    public void addAttributeModifiers(Multimap<Attribute, AttributeModifier> attributes,
                                      SlotContext context, ItemStack stack) {
        if (!isVisionSlot(context)) {
            return;
        }
        addAttributeModifier(attributes, Attributes.ATTACK_DAMAGE,
                id -> new AttributeModifier(id, "Hangman's attack", -0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
        addAttributeModifier(attributes, LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get(),
                id -> new AttributeModifier(id, "Hangman's magic proficiency", 0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
        IronsSpellsCompat.addSpellPowerToCurio(this, attributes, 0.5F);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!isVisionSlot(context)) {
            return;
        }
        LivingEntity wearer = context.entity();
        if (stage(stack) >= 2 && wearer.isUsingItem()
                && wearer.getUseItem().getUseAnimation() == UseAnim.BLOCK) {
            wearer.stopUsingItem();
        }
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltip, ItemStack stack) {
        List<Component> lines = super.getAttributesTooltip(tooltip, stack);
        int stage = stage(stack);
        if (stage >= 1) {
            lines.add(positiveEffect(EFFECT_PREFIX + "trap"));
        }
        if (stage >= 2) {
            lines.add(positiveEffect(EFFECT_PREFIX + "reflect"));
            lines.add(negativeEffect(EFFECT_PREFIX + "restrictions"));
        }
        if (stage >= 3) {
            lines.add(positiveEffect(EFFECT_PREFIX + "relay"));
        }
        if (FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown()) {
            Component flavor = Component.translatable(FLAVOR_TEXT_PREFIX + stage).withStyle(ChatFormatting.DARK_PURPLE);
            if (!flavor.getString().isBlank()) {
                lines.add(flavor);
            }
        }
        return lines;
    }
}
