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

/**
 * Maledict 的符文共用这一个类：符文木的「饱食符文」「衰朽符文」，灵魂木的「汰余符文」「熟成符文」。
 *
 * <h2>符文本身不做任何事</h2>
 * 这一层只有一件事：戴上就给效果、摘下就让它自然过期。动手的全在效果那一边——生灵之祝的算术在
 * {@code BlessedRegeneration} 与 {@code FoodDataMixin} 里，熟成之赐在 {@code RipeningBonus} 与
 * {@code RipeningEvents} 里，衰朽与汰余则直接写在 {@code DecayEffect} / {@code ThinningEffect} 的 tick 里。
 * 这样切的好处是：符文这一侧出问题（被无常没收、被 {@code /effect clear}、跨维度）最多丢 2 秒，
 * 而效果自己既不知道也不关心是谁给它挂上的——命令、别的模组、将来的更高等级来源都一视同仁。
 *
 * <h2>为什么不直接用 Malum 的图腾符文类</h2>
 * Malum 的 {@code TotemicRuneCurioItem} 是从仪式对象里「借」效果的：它把传入的
 * {@code TotemicRiteType} 强转成 {@code PotionRiteEffect} 取出 (效果, 谓词) 对，
 * 转不过去就在构造期抛 {@code IllegalArgumentException("Supplied rite type must have an aura effect")}。
 * 我们这几枚要落地的仪式全都不是药水效果：邪恶线是衰朽扣血、赋能强化怪物、驱魔处决、屠戮宰牲，
 * 神圣线要用的滋养是给牲畜催熟，饱食符文更是自己发明的一份东西——这也是 Malum 没给它们出图腾符文的原因。
 * 所以这一层自己写：精魂当构造参数传进来（神圣精魂或邪恶精魂，决定符文的稀有度底色与屏幕粒子），
 * 效果也当构造参数传进来。
 *
 * <h2>为什么是每 40 tick 给一次 200 tick</h2>
 * 与 Malum 图腾符文完全同频：持续刷新比「装备/卸下各来一次」抗折腾，
 * 死亡、跨维度、被无常没收再归还、{@code /effect clear} 都会在最多 40 tick（2 秒）后自愈。
 * 效果到期自然消失，符文不需要知道玩家是什么时候摘的。
 *
 * <p>刷新走 {@code addEffect} 的既有规则：等级更高的不会被 I 级覆盖，时间更长的也不会被 200 tick 顶掉，
 * 所以符文与其它来源不会互相打架。
 *
 * <h2>说明文字</h2>
 * tooltip 那行蓝字走 Malum 的 {@link #positiveEffect}，传进去的只是后缀，
 * 完整键是 {@code malum.gui.curio.effect.<后缀>}，写在 {@code MaledictLanguage} 里。
 */
public final class PulseRuneItem extends AbstractRuneCurioItem {

    /** 一次给多久。 */
    public static final int EFFECT_DURATION_TICKS = 200;

    /** 多久刷一次。必须小于 {@link #EFFECT_DURATION_TICKS}，否则效果会断档。 */
    public static final int REFRESH_INTERVAL_TICKS = 40;

    /** 符文给的效果等级：等级 I，也就是 amplifier 0。 */
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
