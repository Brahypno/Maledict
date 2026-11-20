package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;

/**
 * 一道刀痕：位置、朝向、运动、寿命与着色种子，全部在创建时定死。
 *
 * <p>旧的裂隙花是"一个实例 = 八瓣同时开合"，贴图帧、透明度、外扩距离都按整个实例算。
 * Malum 的飞腾与隐藏锋刃却是几百道各自错峰出生的刀光，所以这里把<b>单道</b>刀痕显式建模：
 * 每道自己带 {@code startTick}、{@code lifetimeTicks} 与位移初速度，绘制时逐道求值。
 *
 * <p>{@code offset} 是相对本次效果锚点的位移，{@code velocity} 单位是格/tick，
 * 位移按 Lodestone 粒子的生成、延迟和插值顺序推进（见 {@link #travel(float)}）。
 * {@code axisU}/{@code axisV} 是四边形所在平面的两条正交单位基向量，
 * 由 {@link RiftOrientation} 从 Malum 的方向向量与 {@code spinOffset} 换算而来。
 */
public record RiftStroke(
        Vec3 offset,
        Vec3 axisU,
        Vec3 axisV,
        Vec3 velocity,
        float halfExtent,
        float startTick,
        float lifetimeTicks,
        float fadeStart,
        float shaderSeed,
        float variant) {

    private static final double DEGENERATE_AXIS = 1.0E-8;

    public RiftStroke {
        if (!isFinite(offset) || !isFinite(velocity)){
            throw new IllegalArgumentException("Non-finite rift stroke position or velocity");
        }
        if (!isFinite(axisU) || !isFinite(axisV)
            || axisU.lengthSqr() < DEGENERATE_AXIS || axisV.lengthSqr() < DEGENERATE_AXIS){
            throw new IllegalArgumentException("Degenerate rift stroke axes");
        }
        if (!Float.isFinite(halfExtent) || halfExtent <= 0.0F
            || !Float.isFinite(lifetimeTicks) || lifetimeTicks <= 0.0F
            || !Float.isFinite(startTick) || startTick < 0.0F){
            throw new IllegalArgumentException("Invalid rift stroke extent or lifetime");
        }
        if (!Float.isFinite(shaderSeed) || !Float.isFinite(variant) || !Float.isFinite(fadeStart)){
            throw new IllegalArgumentException("Non-finite rift stroke shading value");
        }
    }

    /**
     * 相对本道刀痕自己的年龄：实例年龄减去出生延迟。
     */
    public float localAge(float instanceAge, float partialTick) {
        return instanceAge + partialTick - startTick;
    }

    /**
     * Particle.tick() 先比较旧 age 再递增，age 等于 lifetime 时仍可绘制。
     */
    public boolean visibleAt(float localAge) {
        return localAge >= 0.0F && localAge < lifetimeTicks + 1.0F;
    }

    /**
     * 本道刀痕的生命进度，用于挑贴图帧与透明度。
     */
    public float fraction(float localAge) {
        return IncursusRiftGeometry.clamp01((localAge - 1.0F) / lifetimeTicks);
    }

    public int maskFrame(float localAge, int frameCount) {
        // Lodestone updates the sprite before advancing the particle age.
        int spriteAge = Math.max(0, (int) localAge - 1);
        return (int) (spriteAge * (frameCount - 1) / lifetimeTicks);
    }

    /**
     * 出生延迟 + 寿命：实例整体的存活时长由所有刀痕的这个最大值决定，
     * 不能统一套用普通挥砍的 12 tick，否则三百道连斩的后半段会被提前清掉。
     */
    public float endTick() {
        return startTick + lifetimeTicks;
    }

    /**
     * 出生帧的上一位置和当前位置都是生成点。第一次移动后才在两个 tick 的位置之间插值。
     * Lodestone 的刀光粒子默认摩擦系数是 1，重力是 0。
     */
    public Vec3 travel(float localAge) {
        double ticks = Math.max(0.0, Math.min(localAge - 1.0F, lifetimeTicks));
        return velocity.scale(ticks);
    }

    /**
     * 刀痕在整批里的相对序号，写进顶点 {@code Color.b} 供 shader 做星点分层。
     *
     * <p>单独一刀时恒为 0，与旧裂隙花"只有一瓣就不分层"的算法一致。
     */
    public static float variant(int index, int count) {
        return count <= 1 ? 0.0F : index / (float) (count - 1);
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
