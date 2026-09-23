package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 「兽群符文」：灵魂木那面刻出来的符文，把图腾「赋能仪式」的脉动调了个头——图腾把抗性提升、力量与迅捷
 * 发给 9×9×9 内的<em>怪物</em>，这枚符文把它们发给佩戴者<em>自己的随从</em>。
 *
 * <h2>它不挂药水效果，自己出手</h2>
 * 另外三枚符文走的是 {@link PulseRuneItem} 那条路：符文只把效果挂到佩戴者身上，动手的全在效果那一侧。
 * 这一枚是例外——符文每 {@value #INTERVAL_TICKS} tick 自己扫一圈，把三样效果直接发给随从，
 * 不经过任何中间效果。所以这里既没有 {@code maledict:pack_boon} 这个注册项，也没有对应的效果文案，
 * 三样效果固定 I 级（见 {@link #BOON_AMPLIFIER}），不再跟着佩戴者身上某个效果的等级走。
 *
 * <h2>认谁算「自己的」</h2>
 * 只看 {@link OwnableEntity} 的 {@code getOwnerUUID()} 是否等于佩戴者：驯服的狼、猫、鹦鹉走
 * {@code TamableAnimal}，马、驴、羊驼这些不是 {@code TamableAnimal} 但也认主，一并覆盖。
 * 别人的宠物、野生的狼、没认主的马都不在其中；佩戴者自己也不吃这三样（发的是「随从」）。
 *
 * <p>扫描用 ±16 的方盒、命中再用球判定：方盒的角上离佩戴者二十多格，不该算「16 格内」。
 *
 * <h2>数值与节奏</h2>
 * 每 40 tick 刷一次、每次给 {@value #BOON_DURATION_TICKS} tick（5 秒）：随从跑出半径后最多 5 秒就褪掉，
 * 跑回来最多 2 秒就补上。粒子只在随从<em>本来没有</em>力量的那一刻放一次，
 * 免得每 2 秒对着一群狼撒一遍烟。
 *
 * <p>说明文字那行蓝字走 Malum 的 {@code positiveEffect}，传进去的只是后缀，
 * 完整键是 {@code malum.gui.curio.effect.<后缀>}，写在 {@code MaledictLanguage} 里。
 */
public final class PackRuneItem extends AbstractRuneCurioItem {

    /** 随从离佩戴者多远还算「身边」。 */
    public static final int RADIUS = 16;

    /** 一次给多久。比刷新间隔长，随从跑出半径后还能带走几秒。 */
    public static final int BOON_DURATION_TICKS = 100;

    /** 出手间隔：每 40 tick 扫一次，与 {@link PulseRuneItem} 刷效果的节奏同频。 */
    public static final int INTERVAL_TICKS = 40;

    /** 三样效果的等级：等级 I，也就是 amplifier 0。 */
    public static final int BOON_AMPLIFIER = 0;

    private final String tooltipSuffix;

    public PackRuneItem(Item.Properties properties, MalumSpiritType spiritType, String tooltipSuffix) {
        super(properties, spiritType);
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
        if (wearer.level().isClientSide || wearer.level().getGameTime() % INTERVAL_TICKS != 0) {
            return;
        }

        UUID owner = wearer.getUUID();
        double reach = (double) RADIUS * RADIUS;
        for (LivingEntity beast : wearer.level().getEntitiesOfClass(LivingEntity.class, packArea(wearer))) {
            if (beast.distanceToSqr(wearer) > reach || !belongsTo(beast, owner)) {
                continue;
            }
            bless(beast);
        }
    }

    private static AABB packArea(LivingEntity wearer) {
        return new AABB(wearer.blockPosition()).inflate(RADIUS);
    }

    private static boolean belongsTo(LivingEntity beast, UUID owner) {
        return beast.isAlive() && beast instanceof OwnableEntity owned && owner.equals(owned.getOwnerUUID());
    }

    private static void bless(LivingEntity beast) {
        if (!beast.hasEffect(MobEffects.DAMAGE_BOOST)) {
            ParticleEffectTypeRegistry.HEXING_SMOKE.createEntityEffect(beast,
                    new ColorEffectData(SpiritTypeRegistry.WICKED_SPIRIT.getPrimaryColor()));
        }
        beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
        beast.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
        beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
    }
}
