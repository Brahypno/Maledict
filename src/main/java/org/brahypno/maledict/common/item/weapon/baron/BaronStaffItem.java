package org.brahypno.maledict.common.item.weapon.baron;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.List;

/**
 * 伯爵的礼杖：铁剑档的仪式用剑。主手或副手持有都给移速加成，并把持有者的可见度乘
 * {@link #visibility()} —— 生物要在仇恨范围六成以内才会锁定你，与隐身药水同一条公式，
 * 见 {@link BaronStaffEvents} 与 {@code TargetingConditions#test}。
 *
 * <p>两个乘数都在配置里：{@code baronCeremonialStaff.visibilityMultiplier} 与
 * {@code baronCeremonialStaff.speedBonus}。
 *
 * <p>1.21 侧：剑的攻击力/攻速改成 {@code Item.Properties#attributes(SwordItem.createAttributes(...))}，
 * 耐久仍由 {@code TieredItem} 按铁档自动给 250；移速加成改由 {@link BaronStaffAttributes}
 * 在 {@code ItemAttributeModifierEvent} 上加（配置要延迟读取，不能烤进注册期组件）。
 */
public final class BaronStaffItem extends SwordItem {
    /**
     * 可见度乘数在配置读不到时的退路，与 {@code visibilityMultiplier} 的默认值一致。
     */
    private static final double DEFAULT_VISIBILITY = 0.6D;

    /**
     * 移速加成在配置读不到时的退路，与 {@code speedBonus} 的默认值一致。
     */
    private static final double DEFAULT_SPEED_BONUS = 0.2D;

    public BaronStaffItem() {
        // 原版铁剑：3 + 铁档 2，面板上就是 6 点攻击力、1.6 攻击速度；耐久 250 也来自铁档。
        super(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));
    }

    /**
     * 主手或副手拿着礼杖。可见度由事件读这个方法。
     */
    public static boolean holdsStaff(Player player) {
        Item staff = MaledictItems.BARON_CEREMONIAL_STAFF.get();
        return player.getMainHandItem().getItem() == staff || player.getOffhandItem().getItem() == staff;
    }

    /**
     * 持有时可见度乘数（{@code baronCeremonialStaff.visibilityMultiplier}）。原版检测半径是
     * {@code max(followRange * 可见度, 2)}，所以 0.6 就是「仇恨范围缩到六成」；两格是原版下限，贴脸照样被发现。
     *
     * <p>配置可能在处理器启动前被读取，读不到就退回默认值。
     */
    public static double visibility() {
        try {
            return MaledictConfig.BARON_STAFF_VISIBILITY.get();
        }
        catch (RuntimeException exception) {
            return DEFAULT_VISIBILITY;
        }
    }

    /**
     * 持有时移速加成（{@code baronCeremonialStaff.speedBonus}），读不到就退回默认值。
     */
    static double speedBonus() {
        try {
            return MaledictConfig.BARON_STAFF_SPEED.get();
        }
        catch (RuntimeException exception) {
            return DEFAULT_SPEED_BONUS;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // 数字跟着配置走；玩家的配置与他连的服务器不一致时这里显示的是本地值。
        tooltip.add(Component.translatable("tooltip.maledict.baron_ceremonial_staff.concealment",
                                           Math.round(visibility() * 100.0D)));
    }
}
