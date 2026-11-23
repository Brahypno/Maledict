package org.brahypno.maledict.common;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 两个能力的<b>载体</b>判定，以及武器自身基础攻击伤害的取值。
 *
 * <p>触发条件永远是「这件物品确实是载体」：Maledict 自己的物品标签
 * {@link MaledictTags#LUMENFALL_CARRIERS} / {@link MaledictTags#REFULGENCE_CARRIERS}
 * （也有反照类只认 {@link MaledictTags#REFULGENCE_CARRIERS} 的入口）。标签默认空着生成，
 * 数据包或将来的 Maledict 武器直接挂上去即可，<b>不需要任何兼容前置在场</b>。
 *
 * <p>1.21.1 没有 Tetra，所以 1.20.1 里那条「Tetra 工具最终效果等级」的暴露渠道整条不搬：
 * 能力本身从来不随它开关，缺的只是一个额外渠道，等 Tetra 有 1.21.1 构建时补回 {@code compat/tetra}
 * 即可，判定逻辑不用改。
 */
public final class MaledictAbilities {

    public static boolean hasLumenfall(ItemStack stack) {
        return isCarrier(stack, MaledictTags.LUMENFALL_CARRIERS);
    }

    public static boolean hasRefulgence(ItemStack stack) {
        return isCarrier(stack, MaledictTags.REFULGENCE_CARRIERS);
    }

    private static boolean isCarrier(ItemStack stack, TagKey<Item> tag) {
        return !stack.isEmpty() && stack.is(tag);
    }

    /**
     * 武器自身的基础攻击伤害：{@code 玩家基础值 + 这件武器自己挂的修饰}，不含药水、饰品、临时力量。
     *
     * <p>取值只看物品自己的属性修饰；一件载体没写攻击力修饰时退回玩家空手基准 —— 载体只要真的
     * 存在就该能算账，不能因为「这件载体没写伤害修饰」而把上限算成 0、让能力悄悄失效。
     */
    public static double weaponAttackDamage(ItemStack stack, double playerBaseAttack) {
        Totals totals = accumulate(stack);
        if (!totals.found) {
            return Math.max(0.0D, playerBaseAttack);
        }
        return (playerBaseAttack + totals.addition) * (1.0D + totals.multiplyBase) * (1.0D + totals.multiplyTotal);
    }

    /**
     * 1.21 的属性修饰换了形状：{@code ItemStack#getAttributeModifiers()} 直接给
     * {@link ItemAttributeModifiers}，{@link AttributeModifier} 也变成了 record（{@code amount()} /
     * {@code operation()}），不再有 {@code getAmount()} / {@code getOperation()}。
     */
    private static Totals accumulate(ItemStack stack) {
        Totals totals = new Totals();
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            Holder<Attribute> attribute = entry.attribute();
            if (!attribute.equals(Attributes.ATTACK_DAMAGE)) {
                continue;
            }
            totals.found = true;
            AttributeModifier modifier = entry.modifier();
            switch (modifier.operation()) {
                case ADD_VALUE -> totals.addition += modifier.amount();
                case ADD_MULTIPLIED_BASE -> totals.multiplyBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totals.multiplyTotal += modifier.amount();
            }
        }
        return totals;
    }

    private static final class Totals {
        private boolean found;
        private double addition;
        private double multiplyBase;
        private double multiplyTotal;
    }

    private MaledictAbilities() {
    }
}
