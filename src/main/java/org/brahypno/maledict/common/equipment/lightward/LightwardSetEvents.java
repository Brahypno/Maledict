package org.brahypno.maledict.common.equipment.lightward;

import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import com.sammy.malum.core.handlers.SoulWardHandler;
import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 2 级弃形与 3 级以魂代形。
 *
 * <p>两件套只要灵魂护盾还有一点存量就加 15% 移速；三件套在灵魂护盾尚存时，对
 * {@code LivingDamageEvent} 里的最终伤害再抵消一份，被抵消的部分照 Malum 的规矩从护盾里扣。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightwardSetEvents {
    /** 固定 UUID：每 tick 都按它查重，换 UUID 会叠出一堆修饰符。 */
    private static final UUID SPEED_MODIFIER_ID = UUID.nameUUIDFromBytes(
            "maledict:aufheben_lightward/abnegation_speed".getBytes(StandardCharsets.UTF_8));
    private static final double SPEED_BONUS = 0.15D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Player player = event.player;
        // 只在服务端挂：属性值本来就会同步给本人，客户端再挂一次会被同步包清掉又补回来，
        // 来回之间本地移速会短暂算两遍。
        if (player.level().isClientSide()) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wanted = LightwardSet.grantsSpeedBonus(player);
        boolean present = speed.getModifier(SPEED_MODIFIER_ID) != null;
        if (wanted && !present) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, "Lightward abnegation speed", SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!wanted && present) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    /**
     * <p>挂 {@code LivingDamageEvent} 而不是 {@code LivingHurtEvent}：这个事件里的量已经过了护甲、
     * 附魔保护与伤害吸收的结算，正是会被从生命里扣掉的那份「最终伤害」。Malum 的灵魂护盾在前面的
     * {@code LivingHurtEvent} 里已经跑完，所以这里不再有物理 30% / 魔法 90% 的分流可重复，只补一份
     * 统一比例的抵消。{@link EventPriority#LOWEST} 是为了排在其它同类修改之后。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !LightwardSet.grantsAbnegation(player)) {
            return;
        }
        double reduction = MaledictConfig.LIGHTWARD_ABNEGATION_REDUCTION.get();
        float amount = event.getAmount();
        float absorbed = (float) (amount * reduction);
        if (absorbed <= 0.0F) {
            return;
        }
        SoulWardHandler handler = MalumPlayerDataCapability.getCapability(player).soulWardHandler;
        double integrity = player.getAttributeValue(AttributeRegistry.SOUL_WARD_INTEGRITY.get());
        if (integrity != 0.0D) {
            handler.soulWard = Math.max(0.0D, handler.soulWard - absorbed / integrity);
        } else {
            handler.soulWard = 0.0D;
        }
        event.setAmount(amount - absorbed);
        MalumPlayerDataCapability.syncTrackingAndSelf(player);
    }

    private LightwardSetEvents() {
    }
}
