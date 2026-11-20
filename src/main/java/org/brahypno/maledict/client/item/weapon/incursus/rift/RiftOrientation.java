package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;

/**
 * Malum 刀光四边形的朝向，翻译成两条正交基向量。
 *
 * <p>Malum 的 {@code PointyDirectionalParticleBehavior#render} 先按 {@code direction} 求出
 * {@code yaw = atan2(dx, dz) - 90°}、{@code pitch = atan2(dy, 水平距离)}，再乘上
 * {@code SpinParticleData} 的 {@code spinOffset}（绕四边形局部 X 轴），最后把局部四角
 * {@code (±1, 0, ±1)} 旋转出来。这里把那套四元数换算出两条基向量：
 *
 * <ul>
 *   <li>{@code U} 恰好就是<b>单位方向向量</b>：局部 X 轴转完只剩 {@code direction} 本身；</li>
 *   <li>{@code V} 是局部 Z 轴，随 {@code direction} 的偏航、俯仰和 {@code spinOffset} 一起转。</li>
 * </ul>
 *
 * <p>于是任意一道刀痕都能写成 {@code center ± U·half ± V·half}，与 Malum 逐顶点一致，
 * 同时保留了旧裂隙花"水平腰环"的全部形状：水平方向配 {@code spin = 0} 时
 * {@code U} 就是径向外扩方向、{@code V} 是它的水平垂线，正是旧 {@code emitCorner} 用的那两组。
 */
public final class RiftOrientation {

    /**
     * 单位方向向量；零向量或非有限数退回 +Z，让后面的 atan2 至少是良定义的。
     *
     * <p>方向向量只通过 {@code atan2} 与 {@code horizontalDistance} 进入朝向计算，
     * 长度不影响结果，所以这里可以安全地归一化——位置偏移才需要原始长度，那部分由 pattern 自己保留。
     */
    public static Vec3 safeDirection(Vec3 direction) {
        if (direction == null){
            return new Vec3(0.0, 0.0, 1.0);
        }
        double length = direction.length();
        if (!Double.isFinite(length) || length < 1.0E-6){
            return new Vec3(0.0, 0.0, 1.0);
        }
        return direction.scale(1.0 / length);
    }

    /**
     * 长度参与位移计算的原始方向向量：只在退化时替换。
     *
     * <p>飞腾上挑的纵向间距、隐藏锋刃的散布、旋斩成环前的椭圆，都按原版直接乘这个向量，
     * 归一化会把它们一起缩掉。零向量（抬头/低头到极限）会让整批刀光叠成一个点，
     * 因此退回 +Z 这个良定义的方向。
     */
    public static Vec3 motionDirection(Vec3 direction) {
        if (direction == null){
            return new Vec3(0.0, 0.0, 1.0);
        }
        boolean finite = Double.isFinite(direction.x) && Double.isFinite(direction.y)
                         && Double.isFinite(direction.z);
        return finite && direction.lengthSqr() > 1.0E-12 ? direction : new Vec3(0.0, 0.0, 1.0);
    }

    /**
     * 归一化，退化时返回给定兜底：原版直接 {@code normalize()} 的零向量会让刀痕位置变成 NaN，
     * 而 NaN 顶点会毒掉整块缓冲区，不只是那一道。
     */
    public static Vec3 normalizedOr(Vec3 vector, Vec3 fallback) {
        if (vector == null){
            return fallback;
        }
        double lengthSqr = vector.lengthSqr();
        if (!Double.isFinite(lengthSqr) || lengthSqr < 1.0E-12){
            return fallback;
        }
        return vector.scale(1.0 / Math.sqrt(lengthSqr));
    }

    /**
     * 局部 X 轴：{@code direction} 的单位化。四边形两侧沿它张开。
     */
    public static Vec3 axisU(Vec3 direction) {
        return safeDirection(direction);
    }

    /**
     * 局部 Z 轴，含 {@code spinOffset} 的绕局部 X 轴旋转：{@code spin} 为弧度。
     *
     * <p>{@code spin = 0} 时四边形是"水平刀痕"（法线朝世界 Y），
     * {@code spin = ±90°} 时立起来成为纵斩——Malum 的 {@code setVerticalSlashAngle()} 正是 1.57。
     */
    public static Vec3 axisV(Vec3 direction, double spin) {
        Vec3 unit = safeDirection(direction);
        double yaw = Math.atan2(unit.x, unit.z);
        double pitch = Math.atan2(unit.y, unit.horizontalDistance());
        double sinSpin = Math.sin(spin);
        double cosSpin = Math.cos(spin);
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        return new Vec3(
                sinSpin * sinPitch * sinYaw - cosSpin * cosYaw,
                -sinSpin * cosPitch,
                sinSpin * sinPitch * cosYaw + cosSpin * sinYaw);
    }

    /**
     * {@link #axisV} 的逆运算：由两条基向量反推 {@code spinOffset}，供降级路径还原成 Malum 粒子。
     *
     * <p>{@code V = sin(spin)·A + cos(spin)·B}，其中 {@code A}、{@code B} 只由方向决定且互相正交，
     * 所以一次点积就能把 {@code spin} 取回来。结果与 {@link #axisV} 构成往返恒等。
     */
    public static double spinOf(Vec3 axisU, Vec3 axisV) {
        Vec3 unit = safeDirection(axisU);
        double yaw = Math.atan2(unit.x, unit.z);
        double pitch = Math.atan2(unit.y, unit.horizontalDistance());
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        Vec3 rotated = new Vec3(sinPitch * sinYaw, -cosPitch, sinPitch * cosYaw);
        Vec3 flat = new Vec3(-cosYaw, 0.0, sinYaw);
        return Math.atan2(axisV.dot(rotated), axisV.dot(flat));
    }

    private RiftOrientation() {
    }
}
