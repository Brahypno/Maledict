package org.brahypno.maledict.common.entity.vicissitude;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.brahypno.maledict.Maledict;

/**
 * 和平难度下拒绝新召唤的无常：已经在世界记录里的实体照常加载与跨维度转移。
 *
 * <p>为什么落在这里而不是只看刷怪蛋：刷怪蛋只是其中一条入口。{@code /summon}、结构生成、
 * 别的模组的生成逻辑都绕开物品，但它们全都要经过"实体进世界"这一道。世界记录（
 * {@link VicissitudeVitalityLedger}）里已经有账的 UUID 说明这只 Boss 本来就在这个世界里，
 * 换句话说这是读档或转维度，不能因为那一刻难度是和平就把它吞掉——玩家切回和平难度只是
 * 让它不该被<b>新</b>召唤，不是让存档里的那只消失。
 *
 * <p>刷怪蛋自己另有一道 {@code refusesPeaceful} 检查（见 {@code VicissitudeSpawnEggItem}）：
 * 那道检查让整枚蛋直接 {@code FAIL}、不消耗物品，比"生成出来再被取消"更干净，两者并不重复。
 *
 * <p>1.20.1 侧同名类用的是 Forge 总线与 {@code @Mod.EventBusSubscriber(bus = FORGE)}；
 * 1.21 的 NeoForge 下游戏总线就是默认总线，{@link EventBusSubscriber} 不带 {@code bus} 即可，
 * {@code EntityJoinLevelEvent#loadedFromDisk()} 仍然是同一个方法。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeSpawnEvents {
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof VicissitudeBossEntity boss
            && event.getLevel() instanceof ServerLevel level
            && level.getDifficulty() == Difficulty.PEACEFUL
            && !event.loadedFromDisk()
            && VicissitudeVitalityLedger.get(level).find(boss.getUUID()) == null){
            event.setCanceled(true);
        }
    }

    private VicissitudeSpawnEvents() {}
}
