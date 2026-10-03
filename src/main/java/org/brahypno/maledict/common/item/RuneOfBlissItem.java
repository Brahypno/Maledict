package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

/**
 * 「无忧符文」：与抑郁符文成对的那一半 —— 抑郁把痛磨钝（自己少挨），无忧把两个人的命运绑在一起
 * （谁都躲不掉，而且血差多少就重多少）。
 *
 * <p>符文自己不挂效果也不 tick，全部出手点都在 {@code BlissRuneEvents}（三个伤害事件）与
 * {@code IncursusBladeItem}（抬一档）里；这里只回答一件事：**谁戴着**。
 */
public final class RuneOfBlissItem extends AbstractRuneCurioItem {

    /** tooltip 第一行：按血量比例差增伤。 */
    private static final String DAMAGE_EFFECT = "maledict.bliss.damage";

    /** tooltip 第二行：强制命中。 */
    private static final String FORCED_HIT_EFFECT = "maledict.bliss.forced_hit";

    public RuneOfBlissItem(Item.Properties properties, MalumSpiritType spiritType) {
        super(properties, spiritType);
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(DAMAGE_EFFECT));
        tooltip.accept(positiveEffect(FORCED_HIT_EFFECT));
    }

    /**
     * 戴着没有。不记账、不看是谁的、不看戴了几枚，问一次是一次。
     *
     * <p>参数收 {@link Entity} 而不是 {@link LivingEntity}，是为了让调用方（伤害来源里的攻击者）
     * 直接把 {@code source.getEntity()} 递进来 —— 为空或者不是生物就是 {@code false}。
     */
    public static boolean isEquipped(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity wearer)) {
            return false;
        }
        return CuriosApi.getCuriosInventory(wearer)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof RuneOfBlissItem))
                        .orElse(false);
    }
}
