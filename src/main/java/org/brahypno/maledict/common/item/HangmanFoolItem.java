package org.brahypno.maledict.common.item;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.rite.WisdomSacrifice;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/** One vision curio, with cumulative per-stack stage markers. Stages three and four reserve no abilities. */
public final class HangmanFoolItem extends MalumCurioItem {
    private static final String EFFECT_PREFIX = "maledict.hangman_fool.";
    private static final String FLAVOR_TEXT_PREFIX = "tooltip.maledict.hangman_fool.flavor.";
    public static final String STAGES_TAG = "HangmanFoolStages";
    public static final int MAX_STAGE = 4;
    public static final ResourceLocation STAGE_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "hangman_fool_stage");

    public HangmanFoolItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    public static int stage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        CompoundTag stages = tag.getCompound(STAGES_TAG);
        int stage = 0;
        while (stage < MAX_STAGE && stages.getBoolean("stage_" + (stage + 1))) {
            stage++;
        }
        return stage;
    }

    /** Adds the next marker without replacing names, enchantments, or other item NBT. */
    public static boolean advance(ItemStack stack) {
        int stage = stage(stack);
        if (!(stack.getItem() instanceof HangmanFoolItem) || stage == MAX_STAGE) {
            return false;
        }
        CompoundTag stages = stack.getOrCreateTag().getCompound(STAGES_TAG);
        stages.putBoolean("stage_" + (stage + 1), true);
        stack.getOrCreateTag().put(STAGES_TAG, stages);
        return true;
    }

    public static ItemStack create(int stage) {
        ItemStack stack = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        for (int next = 1; next <= Math.min(MAX_STAGE, stage); next++) {
            advance(stack);
        }
        return stack;
    }

    public static int equippedStage(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(inventory -> {
            var vision = inventory.getCurios().get(WisdomSacrifice.DELUSION_SLOT);
            int stage = -1;
            if (vision != null) {
                var stacks = vision.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof HangmanFoolItem) {
                        stage = Math.max(stage, stage(stack));
                    }
                }
            }
            return stage;
        }).orElse(-1);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return WisdomSacrifice.DELUSION_SLOT.equals(context.identifier());
    }

    @Override
    public void addAttributeModifiers(Multimap<Attribute, AttributeModifier> attributes,
                                      SlotContext context, ItemStack stack) {
        if (!WisdomSacrifice.DELUSION_SLOT.equals(context.identifier())) {
            return;
        }
        addAttributeModifier(attributes, Attributes.ATTACK_DAMAGE,
                id -> new AttributeModifier(id, "Hangman's attack", -0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
        addAttributeModifier(attributes, LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get(),
                id -> new AttributeModifier(id, "Hangman's magic proficiency", 0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!WisdomSacrifice.DELUSION_SLOT.equals(context.identifier())) {
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
        if (FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown()) {
            Component flavor = Component.translatable(FLAVOR_TEXT_PREFIX + stage).withStyle(ChatFormatting.DARK_PURPLE);
            if (!flavor.getString().isBlank()) {
                lines.add(flavor);
            }
        }
        return lines;
    }
}
