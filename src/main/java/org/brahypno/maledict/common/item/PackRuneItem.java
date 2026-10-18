package org.brahypno.maledict.common.item;

import static com.sammy.malum.core.helpers.ComponentHelper.*;
import net.minecraft.core.Holder;
import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
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
 * 「兽群符文」：每 {@value #INTERVAL_TICKS} tick 扫一圈，把图腾「赋能仪式」那三样效果直接发给
 * 佩戴者自己的随从（只看 {@link OwnableEntity} 的 {@code getOwnerUUID()}）。
 */
public final class PackRuneItem extends AbstractRuneCurioItem {

    /** 随从离佩戴者多远还算「身边」。 */
    public static final int RADIUS = 16;

    /** 一次给多久（tick），比刷新间隔长，随从跑出半径后还能带走几秒。 */
    public static final int BOON_DURATION_TICKS = 100;

    /** 出手间隔（tick）。 */
    public static final int INTERVAL_TICKS = 40;

    public static final int BOON_AMPLIFIER = 0;

    private final String tooltipSuffix;

    public PackRuneItem(Item.Properties properties, com.sammy.malum.core.systems.registry.SpiritHolder<SpiritArcanaType> spiritType, String tooltipSuffix) {
        super(properties, spiritType, MalumTrinketType.RUNE);
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
            MalumParticleEffectTypes.ENTITY_RITE_EFFECT.createEffect(beast).color(MalumSpiritTypes.WICKED_SPIRIT.getPrimaryColor()).spawn((net.minecraft.server.level.ServerLevel) beast.level());
        }
        beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
        beast.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
        beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BOON_DURATION_TICKS,
                BOON_AMPLIFIER, true, true));
    }
}
