package org.brahypno.maledict.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import team.lodestar.lodestone.systems.network.WeaponParticleEffectType;

/**
 * 读取 Lodestone 刀光构建器里两个只有 {@code protected} 字段才有的量。
 *
 * <p>为什么需要它：1.8.2 把 Malum 的刀光拆成"服务端构造成 {@code WeaponParticleEffectBuilder}
 * → 发包 → 客户端渲染"，改造点只剩构建器本身，而裂隙渲染需要的方向与镜像标志分别存在
 * {@code direction} / {@code isMirrored} 两个受保护字段里（{@code slashRotation} 有公开 getter，
 * 不用包）。这两个值都是上游在触发那一刻抽样的结果，客户端不能重抽——重抽会改掉
 * {@code level.getRandom()} 的消耗顺序，让别的逻辑抽到不同的数。
 *
 * <p>除此之外不改 Lodestone 的任何行为：只加读访问器，不写、不注入。
 */
@Mixin(value = WeaponParticleEffectType.WeaponParticleEffectBuilder.class, remap = false)
public interface WeaponParticleEffectBuilderAccess {

    /**
     * {@code aimedAt(...)} 原样存下来的朝向：隐藏锋刃那条路径是 {@code attacker.getLookAngle()}。
     */
    @Accessor("direction")
    Vec3 maledict$direction();

    /**
     * {@code mirroredRandomly(random)} 抽样后的镜像标志；强化上挑走 {@code mirrored(true)}。
     */
    @Accessor("isMirrored")
    boolean maledict$isMirrored();
}
