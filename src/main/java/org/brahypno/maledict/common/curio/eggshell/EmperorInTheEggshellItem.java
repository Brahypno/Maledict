package org.brahypno.maledict.common.curio.eggshell;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeMod;
import org.brahypno.maledict.config.MaledictConfig;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 「蛋壳中的皇帝」：受击后开启一段渐变的蛋壳庇护，只削伤害，不额外挂 guard。
 *
 * <p>它不替换原版无敌帧：原版 20 tick 照旧在前，窗口在 {@code LivingHurtEvent} 里对漏进来的
 * 伤害按 {@code elapsed / N} 缩放。{@code N} 由 {@link MaledictConfig#EMPEROR_EGGSHELL_WINDOW_TICKS}
 * 配置，且 {@code N <= 20} 时门会被原版帧盖住，所以配置下限是 21。
 *
 * <p>负面：佩戴时实体和方块交互距离都按基础值 -50%。固定 UUID 让多枚同饰品不会叠加到零。
 */
public final class EmperorInTheEggshellItem extends MalumCurioItem {
    private static final String EFFECT = "maledict.emperor_in_the_eggshell.window";
    private static final String REACH_EFFECT = "maledict.emperor_in_the_eggshell.reach";
    private static final double REACH_PENALTY = -0.5D;
    private static final UUID BLOCK_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/block_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID ENTITY_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/entity_reach".getBytes(StandardCharsets.UTF_8));

    public EmperorInTheEggshellItem() {
        // Charm slot. VOID also gives the endgame trinket equip sound; the slot itself comes from
        // the curios:charm item tag in MaledictItemTags.
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    @Override
    public void addAttributeModifiers(
            Multimap<Attribute, AttributeModifier> attributeMap,
            SlotContext slotContext, ItemStack stack) {
        attributeMap.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(
                BLOCK_REACH_MODIFIER, "maledict:emperor_in_the_eggshell.block_reach",
                REACH_PENALTY, AttributeModifier.Operation.MULTIPLY_TOTAL));
        attributeMap.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                ENTITY_REACH_MODIFIER, "maledict:emperor_in_the_eggshell.entity_reach",
                REACH_PENALTY, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT, MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get()));
    }

    /**
     * Queried on demand, like the other passive curios; no equip/unequip bookkeeping needed.
     */
    public static boolean isEquipped(@Nullable LivingEntity entity) {
        if (entity == null){
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof EmperorInTheEggshellItem))
                        .orElse(false);
    }
}
