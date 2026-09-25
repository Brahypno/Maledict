package org.brahypno.maledict.common.item;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.IVoidItem;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.EnlightenmentLevel;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictMobEffects;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 启蒙之年：护符（Curios 的 charm 槽），戴上后在头部渲染一张面具。
 */
public final class AgeOfEnlightenmentItem extends MalumCurioItem implements IVoidItem {

    private static final String CURIO_SLOT = "curio";

    /** 槽位修饰符的 UUID 由物品名算死：跨存档一致，重复加载不会叠加成两条修饰符。 */
    private static final UUID SLOT_MODIFIER_ID =
            UUID.nameUUIDFromBytes("maledict:age_of_enlightenment/curio_slot".getBytes(StandardCharsets.UTF_8));

    /** 只是后缀：Malum 的 {@link #positiveEffect} 会自己拼上 {@code malum.gui.curio.effect.}。 */
    private static final String EFFECT_PREFIX = "maledict.age_of_enlightenment.";
    private static final String HOLD_SHIFT = "tooltip.maledict.age_of_enlightenment.hold_shift";
    private static final String SHIFT_LINE = "tooltip.maledict.age_of_enlightenment.shift";
    private static final String ENLIGHTENED_SHIFT_LINE = "tooltip.maledict.age_of_enlightenment.shift.enlightened";

    /** 模型覆盖用的物品属性：{@code 1.0} 表示佩戴者身上有启蒙之年，切到第二张贴图。 */
    public static final ResourceLocation ENLIGHTENED_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "enlightened");

    public AgeOfEnlightenmentItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    /** tooltip 里那行「+1 护符槽位」由 Curios 生成，不要覆盖 {@code getSlotsTooltip}，否则会出两行。 */
    @Override
    public void addAttributeModifiers(
            Multimap<Attribute, AttributeModifier> attributeMap,
            SlotContext slotContext, ItemStack stack) {
        CuriosApi.addSlotModifier(attributeMap, CURIO_SLOT, SLOT_MODIFIER_ID, 1.0D,
                                  AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "cooldown"));
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "spirit_void"));
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "enlightenment"));

        if (!Screen.hasShiftDown()){
            tooltip.accept(Component.translatable(HOLD_SHIFT).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable(hasEnlightenment() ? ENLIGHTENED_SHIFT_LINE : SHIFT_LINE)
                                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** 刻意只看本地玩家；服务端永远返回 false，所以 tooltip 逻辑不会去碰客户端类。 */
    private static boolean hasEnlightenment() {
        if (FMLEnvironment.dist != Dist.CLIENT){
            return false;
        }
        Player player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get());
    }

    public static boolean isEquipped(LivingEntity entity) {
        if (entity == null){
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof AgeOfEnlightenmentItem))
                        .orElse(false);
    }

    public static ItemStack create(int level) {
        ItemStack stack = new ItemStack(MaledictItems.AGE_OF_ENLIGHTENMENT.get());
        stack.getOrCreateTag().putInt(EnlightenmentLevel.TAG,
                                      Math.max(EnlightenmentLevel.FALLBACK, level));
        return stack;
    }

    public static int equippedLevel(LivingEntity entity) {
        return EnlightenmentLevel.fromTag(equippedStack(entity).getTag());
    }

    private static ItemStack equippedStack(LivingEntity entity) {
        if (entity == null){
            return ItemStack.EMPTY;
        }
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(entity).orElse(null);
        if (inventory == null){
            return ItemStack.EMPTY;
        }
        for (ICurioStacksHandler stacks : inventory.getCurios().values()) {
            IDynamicStackHandler slots = stacks.getStacks();
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                ItemStack stack = slots.getStackInSlot(slot);
                if (stack.getItem() instanceof AgeOfEnlightenmentItem){
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
