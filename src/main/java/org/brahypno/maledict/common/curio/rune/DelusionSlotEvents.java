package org.brahypno.maledict.common.curio.rune;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.rite.WisdomSacrifice;
import top.theillusivec4.curios.api.CuriosApi;

/** 视界槽存在即免疫黑暗；空槽和关闭渲染开关都保留免疫。 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class DelusionSlotEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.DARKNESS
                && event.getEntity() instanceof Player player && hasVisionSlot(player)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** 获得槽位之前已有的黑暗，在效果迭代结束后清除并由原版同步到客户端。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.hasEffect(MobEffects.DARKNESS) && hasVisionSlot(player)) {
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    private static boolean hasVisionSlot(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.getStacksHandler(WisdomSacrifice.DELUSION_SLOT)
                        .map(stacks -> stacks.getSlots() > 0).orElse(false))
                .orElse(false);
    }

    private DelusionSlotEvents() {
    }
}
