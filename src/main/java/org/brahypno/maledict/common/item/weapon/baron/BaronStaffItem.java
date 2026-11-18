package org.brahypno.maledict.common.item.weapon.baron;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 伯爵的礼杖：铁剑档的仪式用剑。主手或副手持有都给移速加成，并把持有者的可见度乘
 * {@link #visibility()} —— 生物要在仇恨范围六成以内才会锁定你，与隐身药水同一条公式，
 * 见 {@link BaronStaffEvents} 与 {@code TargetingConditions#test}。
 *
 * <p>两个乘数都在配置里：{@code baronCeremonialStaff.visibilityMultiplier} 与
 * {@code baronCeremonialStaff.speedBonus}。
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

    /**
     * 主手、副手各用一枚 UUID。两把礼杖同时拿在手里时，同一个 UUID 会被
     * {@code AttributeInstance#addTransientModifier} 判成重复，所以这里必须分开。
     */
    private static final UUID MAINHAND_SPEED_ID = UUID.fromString("b6a1c3de-6f2b-4a59-9f1e-2c7d8e4f0a11");
    private static final UUID OFFHAND_SPEED_ID = UUID.fromString("3c9d5f27-8a4e-4b13-9c60-7e1f2d4a6b02");
    private static final String SPEED_MODIFIER_NAME = "Baron ceremonial staff speed";

    public BaronStaffItem() {
        // 原版铁剑：3 + 铁档 2，面板上就是 6 点攻击力、1.6 攻击速度；耐久 250 也来自铁档。
        super(Tiers.IRON, 3, -2.4F, new Item.Properties());
    }

    /**
     * 主手或副手拿着礼杖。属性走 {@link #getAttributeModifiers}，可见度由事件读这个方法。
     */
    public static boolean holdsStaff(Player player) {
        Item staff = MaledictItems.BARON_CEREMONIAL_STAFF.get();
        return player.getMainHandItem().getItem() == staff || player.getOffhandItem().getItem() == staff;
    }

    /**
     * 持有时可见度乘数（{@code BARONCeremonialStaff.visibilityMultiplier}）。原版检测半径是
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
    private static double speedBonus() {
        try {
            return MaledictConfig.BARON_STAFF_SPEED.get();
        }
        catch (RuntimeException exception) {
            return DEFAULT_SPEED_BONUS;
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> attributes = super.getAttributeModifiers(slot, stack);
        UUID speedId = slot == EquipmentSlot.MAINHAND ? MAINHAND_SPEED_ID
                                                      : slot == EquipmentSlot.OFFHAND ? OFFHAND_SPEED_ID
                                                                                      : null;
        if (speedId == null){
            return attributes;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(attributes)
                                .put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                                        speedId, SPEED_MODIFIER_NAME, speedBonus(),
                                        AttributeModifier.Operation.MULTIPLY_TOTAL))
                                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 数字跟着配置走；玩家的配置与他连的服务器不一致时这里显示的是本地值。
        tooltip.add(Component.translatable("tooltip.maledict.baron_ceremonial_staff.concealment",
                                           Math.round(visibility() * 100.0D)));
    }
}
