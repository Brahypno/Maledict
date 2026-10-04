package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

/**
 * 「堕落符文」：符文自己不挂效果也不 tick，全部行为在 {@code CorruptionRuneEvents} 里，由事件问一句
 * {@link #isEquipped}。它做两件事 —— 给打中的目标落下 {@code Fallen}（堕落窗口），以及把窗口里攒下的
 * 活性腐败收走。
 *
 * <p>格位与配方见 {@code MaledictItems} / {@code MaledictRecipes}，模型、贴图与典籍页同其它符文一路。
 */
public final class RuneOfTheFallenItem extends AbstractRuneCurioItem {

    private static final String EFFECT_SUFFIX = "maledict.fallen";

    public RuneOfTheFallenItem(Item.Properties properties, MalumSpiritType spiritType) {
        super(properties, spiritType);
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT_SUFFIX));
    }

    public static boolean isEquipped(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof RuneOfTheFallenItem))
                        .orElse(false);
    }
}
