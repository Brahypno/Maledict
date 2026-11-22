package org.brahypno.maledict.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.brahypno.maledict.common.MaledictTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.lodestar.lodestone.handlers.LodestoneAttributeEventHandler;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

/**
 * 把 {@code MAGIC_DAMAGE} 的结算从 Lodestone 手里接过来，交给 ChangeLib 的属性伤害派发器
 * （注册见 {@code MaledictAttributes#registerAttributeDamage}）。
 *
 * <p><b>注入点是 {@code triggerMagicDamage(LivingDamageEvent.Post)}，不是 {@code processAttributes}。</b>
 * 1.8.2 的 {@code LodestoneAttributeEventHandler} 有两个方法：{@code processAttributes(Pre)} 只做魔法抗性 /
 * 精通那两笔缩放，真正的附加魔法伤害在 {@code triggerMagicDamage(Post)} 里
 * {@code DamageTypeHelper.create(MAGIC, attacker)} + {@code victim.hurt(...)} 打出去
 * （javap -c 实读，攻击者为直伤实体）。所以拦 {@code processAttributes} 等于什么都没拦：
 * 本模组那笔由派发器发的魔法伤害照旧出手，Lodestone 这笔也照旧落地，一刀吃两遍
 * （GameTest 里表现为「十一近战 + 一点魔法」实测掉 13 点，比期望多 1）。
 *
 * <p>命中 {@link MaledictTags#LODESTONE_MAGIC_TAKEOVER} 里的武器时取消这一笔；
 * {@code IS_MAGIC} 分支刻意放过：本模组自己的 {@code minecraft:magic} 来源仍走 Lodestone 的魔法抗性
 * （那条走 {@code processAttributes}，本 mixin 不碰）。
 *
 * <p><b>不变式：这里的三个条件必须与 {@code MaledictAttributes#allowsMagicTakeover} 逐字一致。</b>
 * 两边判定同一件事（哪个来源、哪把武器），所以「Lodestone 会发一笔」与「派发器会发一笔」严格同真同假：
 * 条件写成一样就既不会归零也不会翻倍。改一边必须同时改另一边。
 */
@Mixin(LodestoneAttributeEventHandler.class)
public abstract class LodestoneAttributeEventHandlerMixin {
    @Inject(method = "triggerMagicDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void maledict$takeOverMagicForTaggedWeapons(LivingDamageEvent.Post event, CallbackInfo callback) {
        DamageSource source = event.getSource();
        if (!source.is(LodestoneDamageTypeTags.CAN_TRIGGER_MAGIC)
                || source.is(LodestoneDamageTypeTags.IS_MAGIC)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!attacker.getMainHandItem().is(MaledictTags.LODESTONE_MAGIC_TAKEOVER)) {
            return;
        }
        callback.cancel();
    }
}
