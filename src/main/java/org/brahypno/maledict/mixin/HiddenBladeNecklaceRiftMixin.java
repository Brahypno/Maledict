package org.brahypno.maledict.mixin;

import com.sammy.malum.common.item.curiosities.curios.sets.weeping.CurioHiddenBladeNecklace;
import com.sammy.malum.core.handlers.SoulDataHandler;
import com.sammy.malum.visual_effects.networked.MalumNetworkedWeaponParticleEffectType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.network.MaledictNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 隐藏锋刃项链：把 Malum 那一次"向前方甩出刀光"的粒子发送换成裂隙通知。
 *
 * <p><b>为什么 1.21 仍然必须是 mixin。</b>1.8.2 没有给"某个物品的响应器要放什么粒子"留事件：
 * 连斩的判定、冷却、WICKED_INTENT 消耗、{@code HiddenBladeDelayedImpactEntity} 的生成都写在
 * {@code CurioHiddenBladeNecklace#outgoingDamageEvent} 里，视觉部分只是这个方法末尾的一串
 * {@code HIDDEN_BLADE_COUNTER_FLURRY.createEffect()...spawn(ServerLevel)}。上游能拦的位置不存在，
 * 于是按 AGENTS.md「mixin 是最后手段」的要求，先查证了上游（{@code MalumParticleEffectTypes} 的
 * 发送没有事件、{@code IMalumEventResponder} 也只是回调物品自己），再把拦截点放到<b>这一行调用</b>：
 * 只替换这一次发送，连斩实体、冷却、音效、伤害结算全部原样执行。
 *
 * <p>与 1.20.1 那版的差别：1.20.1 拦的是 {@code ParticleHelper.SlashParticleEffectBuilder}
 * （{@code spawnForwardSlashingParticle(Entity, Vec3)}），方向与角度沿参数拿；1.8.2 换成了
 * {@code WeaponParticleEffectBuilder}，参数只有 {@code ServerLevel}，所以方向、基础角、镜像
 * 改从构建器里读（见 {@link WeaponParticleEffectBuilderAccess}），这三个量都是上游已经抽好的样本，
 * 客户端不再重抽。
 *
 * <p>锚点按 Malum 自己的 {@code getEffectPosition} 公式：来源实体（施法者）位置 + 半身高，
 * 这条调用没有设置任何偏移量。没有神侵恶刃时原样转调原方法，其它镰刀、其它武器、
 * 以及任何非玩家来源的行为都不变。
 *
 * <p>mixin 注册在公共 {@code mixins} 数组而不是 {@code client}：这段逻辑跑在服务端，
 * 独立服务器必须能加载它。
 */
@Mixin(value = CurioHiddenBladeNecklace.class, remap = false)
public abstract class HiddenBladeNecklaceRiftMixin {

    @Redirect(
            method = "outgoingDamageEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$"
                             + "MalumWeaponParticleEffectBuilder;spawn(Lnet/minecraft/server/level/ServerLevel;)"
                             + "Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$"
                             + "MalumWeaponParticleEffectBuilder;"),
            remap = false)
    private MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder<?> maledict$replaceHiddenBladeFlurry(
            MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder<?> builder,
            ServerLevel level,
            LivingDamageEvent.Pre event,
            LivingEntity attacker,
            LivingEntity victim,
            ItemStack stack) {
        ItemStack scythe = SoulDataHandler.getScytheWeapon(event.getSource(), attacker);
        if (!(scythe.getItem() instanceof IncursusBladeItem) || !(attacker instanceof ServerPlayer player)) {
            return builder.spawn(level);
        }

        WeaponParticleEffectBuilderAccess access = (WeaponParticleEffectBuilderAccess) builder;
        Vec3 center = attacker.position().add(0.0, attacker.getBbHeight() * 0.5, 0.0);
        // 关键：不转调原方法。客户端会按同一套公式生成 300 道裂隙，替代原本 300 个粒子。
        MaledictNetwork.sendIncursusRiftSkillNear(
                player, RiftMode.HIDDEN_BLADE_FLURRY, center, access.maledict$direction(),
                builder.getSlashRotation(), access.maledict$isMirrored());
        return builder;
    }
}
