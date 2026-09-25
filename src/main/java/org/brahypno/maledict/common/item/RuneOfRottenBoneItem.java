package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

/**
 * 「朽骨符文」：白镴符板那面刻出来的第二枚符文，把 {@code malum:living_flesh} 喂给恶念白镴得到的
 * 一份「替你死」的账。
 *
 * <h2>符文本身只做一件事：报出自己还在不在</h2>
 * 其余三枚图腾符文走 {@link PulseRuneItem} 那条路（戴着手就刷药水效果），
 * 这枚和 {@link AttributeRuneItem} 一样不挂效果——免死这件事只在死亡的瞬间发生一次，
 * 没有可以「持续刷新」的东西。真正动手的是 {@link org.brahypno.maledict.common.curio.RottenBoneEvents}：
 * 它在 {@code LivingDeathEvent} 里问一句 {@link #isEquipped}，然后决定是接住这一死还是放它过去。
 *
 * <p>数值与账目都在 {@link org.brahypno.maledict.common.curio.RottenBone} 里，这一层只有物品本身。
 *
 * <h2>说明文字</h2>
 * tooltip 那行蓝字走 Malum 的 {@code positiveEffect}，传进去的只是后缀，
 * 完整键是 {@code malum.gui.curio.effect.<后缀>}，写在 {@code MaledictLanguage} 里。
 * 数字写在那一行里（四分之一、7 点生命、3.5 秒、四次），法典那一页只讲为什么。
 */
public final class RuneOfRottenBoneItem extends AbstractRuneCurioItem {

    /** tooltip 那行蓝字的后缀，见类注释。 */
    private static final String EFFECT_SUFFIX = "maledict.rotten_bone";

    public RuneOfRottenBoneItem(Item.Properties properties, MalumSpiritType spiritType) {
        super(properties, spiritType);
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT_SUFFIX));
    }

    /**
     * 佩戴者身上有没有这枚符文。
     *
     * <p>与 {@code AgeOfEnlightenmentItem#isEquipped} 同一写法：交给 Curios 自己的谓词，
     * 槽位解锁、数量修正、被无常临时没收这些事都由它处理，比我们遍历 {@code getCurios()} 更准。
     */
    public static boolean isEquipped(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof RuneOfRottenBoneItem))
                        .orElse(false);
    }
}
