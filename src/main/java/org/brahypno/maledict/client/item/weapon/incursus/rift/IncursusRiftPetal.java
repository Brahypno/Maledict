package org.brahypno.maledict.client.item.weapon.incursus.rift;

/**
 * 单瓣在创建时固定的随机参数；同一挥砍的每一瓣只由种子派生一次，不逐帧重抽。
 *
 * <p>取值范围照抄旧效果的两次 {@code RandomHelper.randomBetween}：
 * 尺寸 2.0–3.0 × 1.5 = 3.0–4.5 格（基准 3.75 × 0.8–1.2），速度 0.3–0.4 格/tick（基准 0.35 × 0.857–1.143）。
 *
 * @param angle      这一瓣在世界 XZ 平面上的外扩方向
 * @param sizeScale  相对旧效果 quadSize 中值的倍率
 * @param speedScale 相对旧效果每 tick 位移中值的倍率
 * @param spin       屏面内自旋；旧效果用 {@code SpinParticleData.create(0)}，恒为 0
 * @param shaderSeed 交给 shader 做星点与断裂扰动
 */
public record IncursusRiftPetal(
        int index,
        double angle,
        float sizeScale,
        float speedScale,
        float spin,
        float shaderSeed) {

    /**
     * 旧效果八段同一 tick 生成、严格 45° 等分，没有波次延迟。
     */
    public static final float MAX_PHASE_OFFSET = 0.0F;

    public static IncursusRiftPetal derive(long swingSeed, int index, int petalCount) {
        long seed = IncursusRiftGeometry.petalSeed(swingSeed, index);
        float phase = (IncursusRiftGeometry.hash01(seed, 1) - 0.5F) * 2.0F * MAX_PHASE_OFFSET;
        double angle = IncursusRiftGeometry.petalAngle(index, petalCount, phase);
        float size = 0.8F + 0.4F * IncursusRiftGeometry.hash01(seed, 2);
        float speed = 0.857F + 0.286F * IncursusRiftGeometry.hash01(seed, 3);
        float shaderSeed = IncursusRiftGeometry.hash01(seed, 5);
        return new IncursusRiftPetal(index, angle, size, speed, 0.0F, shaderSeed);
    }
}
