package org.brahypno.maledict.common;

import com.google.common.collect.Multimap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.compat.tetra.TetraCompat;

import java.util.Map;

/**
 * 两个能力的<b>载体</b>判定，以及武器自身基础攻击伤害的取值。
 *
 * <p><b>能力本身不随 Tetra 开关。</b> Tetra 只是把能力暴露到工具上的其中一条渠道（材料 → 强化 →
 * {@code ItemEffect}）。触发条件永远是「这件物品确实是载体」：
 * <ul>
 *   <li>Maledict 自己的物品标签 {@link MaledictTags#LUMENFALL_CARRIERS} /
 *       {@link MaledictTags#REFULGENCE_CARRIERS}：默认空着生成，数据包或将来的 Maledict 武器直接
 *       挂上去即可，<b>不需要 Tetra 在场</b>；</li>
 *   <li>Tetra 若在场，再认一次工具最终效果等级（{@code maledict.lumenfall} /
 *       {@code maledict.refulgence}）。</li>
 * </ul>
 * 所以没装 Tetra 的服上，只要有人把物品挂进标签，能力就照常积蓄、充能、释放；只是默认没人挂。
 */
public final class MaledictAbilities {

    public static boolean hasLumenfall(ItemStack stack) {
        return isCarrier(stack, MaledictTags.LUMENFALL_CARRIERS) || TetraCompat.hasLumenfall(stack);
    }

    public static boolean hasRefulgence(ItemStack stack) {
        return isCarrier(stack, MaledictTags.REFULGENCE_CARRIERS) || TetraCompat.hasRefulgence(stack);
    }

    private static boolean isCarrier(ItemStack stack, TagKey<Item> tag) {
        return !stack.isEmpty() && stack.is(tag);
    }

    /**
     * 武器自身的基础攻击伤害：{@code 玩家基础值 + 这件武器自己挂的修饰}，不含药水、饰品、临时力量。
     *
     * <p>取值顺序：物品自己的属性修饰（普通物品、以及任何自己声明攻击力的载体）→ Tetra 的模块属性
     * （模块化工具把攻击力放在模块上，不经过物品的修饰表）→ 玩家空手基准。
     * 最后那一步是故意的：载体只要真的存在就该能算账，不能因为「没装 Tetra」或「这件载体没写伤害
     * 修饰」而把上限算成 0、让能力悄悄失效。
     */
    public static double weaponAttackDamage(ItemStack stack, double playerBaseAttack) {
        Totals totals = accumulate(stack.getAttributeModifiers(EquipmentSlot.MAINHAND));
        if (!totals.found) {
            totals = accumulate(TetraCompat.moduleAttributes(stack));
        }
        if (!totals.found) {
            return Math.max(0.0D, playerBaseAttack);
        }
        return (playerBaseAttack + totals.addition) * (1.0D + totals.multiplyBase) * (1.0D + totals.multiplyTotal);
    }

    private static Totals accumulate(Multimap<Attribute, AttributeModifier> modifiers) {
        Totals totals = new Totals();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (entry.getKey() != Attributes.ATTACK_DAMAGE) {
                continue;
            }
            totals.found = true;
            AttributeModifier modifier = entry.getValue();
            switch (modifier.getOperation()) {
                case ADDITION -> totals.addition += modifier.getAmount();
                case MULTIPLY_BASE -> totals.multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> totals.multiplyTotal += modifier.getAmount();
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
