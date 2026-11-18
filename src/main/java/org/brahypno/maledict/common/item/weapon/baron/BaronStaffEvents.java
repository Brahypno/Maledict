package org.brahypno.maledict.common.item.weapon.baron;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

/**
 * 伯爵的礼杖的「被发现」：在 Forge 的可见度事件上把持有者的可见度乘 {@link BaronStaffItem#visibility()}。
 *
 * <p>{@code LivingEvent.LivingVisibilityEvent} 由 {@code ForgeHooks.getEntityVisibilityMultiplier}
 * 在 {@code LivingEntity#getVisibilityPercent} 的末尾发布（乘数已经被原版算好：隐身按护甲覆盖率打折、
 * 戴对应生物头颅再打一档），{@code modifyVisibility} 是乘法叠加，Forge 再把结果下限截到 0。
 * 生物的索敌半径在 {@code TargetingConditions#test} 里算成
 * {@code max(followRange × 可见度, 2.0)}，所以乘数 {@link BaronStaffItem#visibility()}（默认 0.6）
 * 就等于「生物要在仇恨范围六成以内才会锁定你」，与隐身药水走同一格，也保留两格贴脸下限。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BaronStaffEvents {
    @SubscribeEvent
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && BaronStaffItem.holdsStaff(player)){
            event.modifyVisibility(BaronStaffItem.visibility());
        }
    }

    private BaronStaffEvents() {
    }
}
