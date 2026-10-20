package org.brahypno.maledict.common.item.rune;

import net.minecraft.core.Holder;
import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;


/**
 * 属性符文：戴上给佩戴者挂一条属性修饰符，摘下由 Curios 收回。
 * <p>魔法抗性在 Lodestone 里是除数（{@code amount / max(resistance, 0.01)}），基础值 1：
 * 修饰符只是把抗性往上加，实际减伤是 {@code 1 - 1 / 抗性}，两个数不是一回事。
 */
public final class AttributeRuneItem extends AbstractRuneCurioItem {
    private final Holder<Attribute> attribute;

    /** 只有 {@code /attribute} 与调试界面看得见，不翻译。 */
    private final String modifierName;

    private final double amount;
    private final AttributeModifier.Operation operation;

    public AttributeRuneItem(Item.Properties properties, com.sammy.malum.core.systems.registry.SpiritHolder<SpiritArcanaType> spiritType,
                             Holder<Attribute> attribute, String modifierName,
                             double amount, AttributeModifier.Operation operation) {
        super(properties, spiritType, MalumTrinketType.RUNE);
        this.attribute = attribute;
        this.modifierName = modifierName;
        this.amount = amount;
        this.operation = operation;
    }

    @Override
    public void addAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> attributeMap,
                                      SlotContext slotContext, ItemStack stack) {
        addAttributeModifier(attributeMap, attribute,
                             new AttributeModifier(net.minecraft.resources.ResourceLocation.parse("maledict:stagnant_evolution"), amount, operation));
    }
}
