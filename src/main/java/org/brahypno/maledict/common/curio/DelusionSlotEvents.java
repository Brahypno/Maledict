package org.brahypno.maledict.common.curio;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.rite.WisdomSacrifice;
import top.theillusivec4.curios.api.CuriosApi;

/** 视界槽存在即免疫黑暗；空槽和关闭渲染开关都保留免疫。 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DelusionSlotEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.DARKNESS
                && event.getEntity() instanceof Player player && hasVisionSlot(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** 获得槽位之前已有的黑暗，在效果迭代结束后清除并由原版同步到客户端。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
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
