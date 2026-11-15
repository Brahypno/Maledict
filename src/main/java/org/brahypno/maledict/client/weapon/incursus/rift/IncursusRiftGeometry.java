package org.brahypno.maledict.client.weapon.incursus.rift;

/**
 * 裂隙花的纯数学层：只做数值计算，不触碰世界、网络或 GPU，因此可以直接单测。
 *
 * <p>旧斩击是 8 张 {@code malum:slash/heavy_slash_0X} 贴图按 45° 交叠出的黑花。这里不重建
 * 花瓣网格，只负责复现它的节奏：每瓣的固定随机量、向外位移、开合、透明度与动画帧号。
 * 形态本身仍由同一套刀痕贴图承担。
 *
 * <p>所有公式都对齐旧效果实测值，不是新设计：寿命 5 tick × 2.5 = 12 tick，
 * 位移走原版 {@code Particle.tick()} 的 0.98 摩擦积分，透明度整段为 1。
 */
public final class IncursusRiftGeometry {
    /** 旧效果 RING_SEGMENTS；质量档位只能在此基础上增减。 */
    public static final int DEFAULT_PETAL_COUNT = 8;
    /** 旧贴图序列 {@code heavy_slash_00..03} 末帧重复，共 5 帧。 */
    public static final int MASK_FRAMES = 5;
    /** 旧效果 RING_RADIUS：花瓣起点到花心的距离。 */
    public static final double ROOT_RADIUS = 0.5;
    /** 原版 {@code Particle.friction}：每 tick 位移后速度乘以它。 */
    public static final float PARTICLE_FRICTION = 0.98F;

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    /** 单瓣种子只由挥砍种子与序号决定，保证同一挥砍在所有客户端形状一致。 */
    public static long petalSeed(long swingSeed, int index) {
        long mixed = swingSeed + GOLDEN * (index + 1L);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    /** 由种子取 [0,1) 的稳定随机数；同一输入永远同一输出，不在每帧重新抽取。 */
    public static float hash01(long seed, int salt) {
        long mixed = seed + GOLDEN * (salt + 1L);
        mixed = (mixed ^ (mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xC4CEB9FE1A85EC53L;
        mixed = mixed ^ (mixed >>> 33);
        return (mixed >>> 40) / (float) (1 << 24);
    }

    /** 第 index 瓣的中心角；旧效果是严格 45° 等分，相位偏移恒为 0。 */
    public static double petalAngle(int index, int petalCount, float phaseOffset) {
        int count = Math.max(1, petalCount);
        return index * Math.PI * 2.0 / count + phaseOffset;
    }

    public static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }

    public static float smoothstep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) {
            return value < edge0 ? 0.0F : 1.0F;
        }
        float t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * 向外位移：复现原版 {@code Particle.tick()} 的积分——每 tick 先 {@code pos += v}，
     * 再 {@code v *= friction}，所以 n tick 后走过的距离是 {@code v0 · (1 - 0.98^n) / 0.02}。
     *
     * <p>旧粒子正是这样：起步快、随后收敛，整条寿命只走完 {@code v0} 的十点七倍左右，
     * 而不是 {@code v0 × lifetime}。按线性还原会让花明显开得更大、更"匀速"。
     */
    public static float radialDistance(float ageFraction, int lifetimeTicks, float speedPerTick) {
        double ticks = clamp01(ageFraction) * Math.max(0, lifetimeTicks);
        double damping = 1.0 - Math.pow(PARTICLE_FRICTION, ticks);
        return (float) (speedPerTick * damping / (1.0 - PARTICLE_FRICTION));
    }

    /** 开合包络：旧效果的 quadSize 全程恒定，所以这里恒为 1；保留接口给后续调参。 */
    public static float openEnvelope(float ageFraction) {
        return 1.0F;
    }

    /**
     * 透明度：旧效果整段不透明（基础 {@code create(1.0f, 0.9f)} 被工程乘到远超 1），到点直接消失。
     *
     * <p>{@code fadeStart} 之后才开始渐隐，默认 0.95 基本等于"到点消失"；
     * 想要柔和收尾就把它调低，这条曲线就是为那一步留的。
     */
    public static float alpha(float ageFraction, float fadeStart) {
        float t = clamp01(ageFraction);
        float fade = Math.max(clamp01(fadeStart), 0.05F);
        if (t <= fade) {
            return 1.0F;
        }
        return 1.0F - smoothstep(fade, 1.0F, t);
    }

    /** 动画帧号；与旧贴图 WITH_AGE 采样一致，按寿命等分切段。 */
    public static int maskFrame(float ageFraction, int frameCount) {
        int frames = Math.max(1, frameCount);
        int frame = (int) (clamp01(ageFraction) * frames);
        return Math.min(frame, frames - 1);
    }

    /** 单瓣半径：旧效果全程恒定，末端不放大也不缩小。 */
    public static float halfExtent(float ageFraction, float baseHalfExtent) {
        return baseHalfExtent * openEnvelope(ageFraction);
    }

    private IncursusRiftGeometry() {
    }
}
