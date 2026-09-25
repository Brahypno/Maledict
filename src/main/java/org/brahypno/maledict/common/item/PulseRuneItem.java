package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Maledict 里「挂效果」那一类符文共用这个类：戴上就给效果、摘下就让它自然过期。 */
public final class PulseRuneItem extends AbstractRuneCurioItem {

    /** 一次给多久（tick）。 */
    public static final int EFFECT_DURATION_TICKS = 200;

    /** 多久刷一次（tick）。必须小于 {@link #EFFECT_DURATION_TICKS}，否则效果会断档。 */
    public static final int REFRESH_INTERVAL_TICKS = 40;

    public static final int EFFECT_AMPLIFIER = 0;

    private final Supplier<MobEffect> effect;
    private final String tooltipSuffix;

    public PulseRuneItem(Item.Properties properties, MalumSpiritType spiritType,
                         Supplier<MobEffect> effect, String tooltipSuffix) {
        super(properties, spiritType);
        this.effect = effect;
        this.tooltipSuffix = tooltipSuffix;
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(tooltipSuffix));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        super.curioTick(slotContext, stack);

        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide || wearer.level().getGameTime() % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        wearer.addEffect(new MobEffectInstance(effect.get(),
                EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, true));
    }
}
