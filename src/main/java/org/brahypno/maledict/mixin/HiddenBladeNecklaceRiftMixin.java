package org.brahypno.maledict.mixin;

import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.common.item.curiosities.curios.sets.weeping.CurioHiddenBladeNecklace;
import com.sammy.malum.core.handlers.SoulDataHandler;
import com.sammy.malum.core.helpers.ParticleHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.network.MaledictNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 隐藏锋刃项链：把 Malum 那一次"向前方甩出刀光"的粒子发送换成裂隙通知。
 *
 * <p>为什么必须落在 Malum 的 {@code hurtEvent} 里面：连斩是否触发由它自己判断——饰品在不在、
 * 冷却、{@code WICKED_INTENT}、伤害类型、飞镰来源——而且它最后会把事件取消掉。
 * 在外部再挂一个 {@code LivingHurtEvent}，要么原版与裂隙同时显示，要么因为优先级与取消机制
 * 得到不稳定结果；取消 Malum 整个方法又会把连斩本体一起弄没。
 * 唯一安全的做法是在这行调用处替换<b>这一次</b>发送，其余流程（连斩实体、冷却、音效、取消）原样执行。
 *
 * <p>目标方法名是编译器为 {@code hurtEvent} 生成的合成 lambda（Malum 6646111 中为
 * {@code lambda$hurtEvent$1}），其中 {@code spawnForwardSlashingParticle(Entity, Vec3)} 只出现一次
 * （字节码 421 行）。Mixin 注册在公共 {@code mixins} 数组而不是 {@code client}：
 * 这段逻辑跑在服务端，独立服务器必须能加载它。
 *
 * <p>没有神侵恶刃时原样转调原方法，其它镰刀、其它武器、以及任何非玩家来源的行为都不变。
 */
@Mixin(value = CurioHiddenBladeNecklace.class, remap = false)
public abstract class HiddenBladeNecklaceRiftMixin {

    @Redirect(
            method = "lambda$hurtEvent$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/core/helpers/ParticleHelper$SlashParticleEffectBuilder;"
                             + "spawnForwardSlashingParticle(Lnet/minecraft/world/entity/Entity;"
                             + "Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false)
    private static void maledict$replaceHiddenBladeFlurry(
            ParticleHelper.SlashParticleEffectBuilder builder, Entity damageDealer, Vec3 direction,
            Level level, LivingEntity attacker, DamageSource source,
            LivingHurtEvent event, MalumLivingEntityDataCapability capability) {
        ItemStack scythe = SoulDataHandler.getScytheWeapon(source, attacker);
        if (level.isClientSide()
            || !(scythe.getItem() instanceof IncursusBladeItem)
            || !(attacker instanceof ServerPlayer player)){
            builder.spawnForwardSlashingParticle(damageDealer, direction);
            return;
        }

        // 与 ParticleHelper#spawnForwardSlashingParticle(Entity, Vec3) 逐行相同的偏移公式：
        // 落点是"伤害来源"（近战是玩家，飞镰是弹射物）身上、抬到它半身高度的位置。
        double yaw = Math.atan2(direction.x, direction.z);
        Vec3 left = new Vec3(-Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 up = left.cross(direction);
        Vec3 offset = direction.scale(0.4).add(up.scale(-0.3));
        if (builder.horizontalOffset != 0.0F){
            offset = offset.add(left.scale(builder.horizontalOffset));
        }
        Vec3 center = damageDealer.position().add(
                offset.x, offset.y + damageDealer.getBbHeight() * 0.5, offset.z);

        // 基础角与镜像由 Malum 在本次触发时抽好（setRandomSlashAngle / mirrorRandomly），
        // 直接沿用，客户端不再重抽，角度分布与改造前一致。
        MaledictNetwork.sendIncursusRiftSkillNear(
                player, RiftMode.HIDDEN_BLADE_FLURRY, center, direction,
                builder.slashAngle, builder.isMirrored);
        // 关键：不转调原方法。客户端会按同一套公式生成 300 道裂隙，替代原本 300 个粒子。
    }
}
