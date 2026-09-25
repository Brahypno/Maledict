package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

/**
 * 「朽骨符文」：符文自己不挂效果也不 tick，免死只在死亡瞬间发生一次，由
 * {@code RottenBoneEvents} 在 {@code LivingDeathEvent} 里问一句 {@link #isEquipped}。
 */
public final class RuneOfRottenBoneItem extends AbstractRuneCurioItem {

    private static final String EFFECT_SUFFIX = "maledict.rotten_bone";

    public RuneOfRottenBoneItem(Item.Properties properties, MalumSpiritType spiritType) {
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
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof RuneOfRottenBoneItem))
                        .orElse(false);
    }
}
