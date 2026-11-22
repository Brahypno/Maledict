package org.brahypno.maledict.client.item.weapon.incursus.rift;

/**
 * 裂隙花的纯数学层：只做数值计算，不触碰世界、网络或 GPU，因此可以直接单测。
 *
 * <p>旧斩击是 8 张 {@code malum:slash/heavy_slash_0X} 贴图按 45° 交叠出的黑花。这里不重建
 * 花瓣网格，只负责复现它的节奏：每瓣的固定随机量、向外位移、开合、透明度与动画帧号。
 * 形态本身仍由同一套刀痕贴图承担。
 *
 * <p>所有公式都对齐旧效果实测值，不是新设计：寿命 5 tick × 2.5 = 12 tick，
 * 位移走原版 {@code Particle.tick()} 的 0.98 摩擦积分，透明度整段为 1。
 *
 * <p>但整朵花按这一次挥砍的<b>伤害半径</b>归一化（{@link #petalDistance}）：花心到末帧花瓣中心
 * 的那圈正好落在服务端真正能打到的球半径上，开合的节奏不变，大小不再由蓄力决定。
 *
 * <p>蓄力改的是<b>薄厚与收尾</b>（{@link #opacityForCharge}、{@link #lifetimeForCharge}）：
 * 轻点又薄又短，满蓄力满不透明、完整绽开。这两条都不碰半径——口径一旦被蓄力接管，玩家就会
 * 重新看到「画得比打得到处大」。
 */
public final class IncursusRiftGeometry {
    /**
     * 旧效果 RING_SEGMENTS；质量档位只能在此基础上增减。
     */
    public static final int DEFAULT_PETAL_COUNT = 8;
    /**
     * 旧贴图序列 {@code heavy_slash_00..03} 末帧重复，共 5 帧。
     */
    public static final int MASK_FRAMES = 5;
    /**
     * 旧效果 RING_RADIUS：花瓣起点到花心的距离。
     */
    public static final double ROOT_RADIUS = 0.5;
    /**
     * 原版 {@code Particle.friction}：每 tick 位移后速度乘以它。
     */
    public static final float PARTICLE_FRICTION = 0.98F;
    /**
     * 刀痕尺寸的基准半径：默认配置（12 tick 寿命、0.35 格/tick 漂移）下满生命周期的花瓣中心圆。
     * 它只负责把刀痕按「本次半径 / 它」等比缩放，本身不决定粗细——粗细由客户端配置
     * {@code incursusRift.petalSize} 决定（默认 2.5 格半边）。
     */
    public static final double AUTHORED_RING_RADIUS = ROOT_RADIUS + radialDistance(1.0F, 12, 0.35F);
    /**
     * 轻点的不透明度：0.30 是改造前 {@code 0.3 + 0.7 × 蓄力} 那条尺寸曲线的最低点，沿用它的手感。
     * 八片花瓣同平面重叠，实际观感比这个数更实（{@code 1-(1-a)^n}），所以不必压得更低。
     */
    public static final float MIN_OPACITY = 0.30F;
    /**
     * 轻点的寿命倍率：默认 12 tick 会缩到 7 tick，读作「一闪即收」，同时半径仍然开到最大。
     */
    public static final float MIN_LIFETIME_FACTOR = 0.55F;
    /**
     * 寿命下限，与客户端配置 {@code lifetimeTicks} 的最小值一致：再短就只剩一两帧贴图。
     */
    public static final int MIN_LIFETIME_TICKS = 4;

    private static final double DEGENERATE_RING = 1.0E-6;

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    /**
     * 报文里的蓄力归一化：非有限值按满蓄力处理——坏包不该让效果凭空变淡，画不画已经由半径那道闸决定。
     */
    public static float normalizeCharge(float raw) {
        return Float.isFinite(raw) ? clamp01(raw) : 1.0F;
    }

    /**
     * 蓄力 → 不透明度倍率：轻点 {@link #MIN_OPACITY}，满蓄力 1。
     */
    public static float opacityForCharge(float charge) {
        float normalized = normalizeCharge(charge);
        return MIN_OPACITY + (1.0F - MIN_OPACITY) * normalized;
    }

    /**
     * 蓄力 → 寿命倍率：轻点 {@link #MIN_LIFETIME_FACTOR}，满蓄力 1。开合本身按寿命归一化，
     * 所以缩短寿命只让花收得更快，末帧仍落在伤害半径上。
     */
    public static float lifetimeFactorForCharge(float charge) {
        float normalized = normalizeCharge(charge);
        return MIN_LIFETIME_FACTOR + (1.0F - MIN_LIFETIME_FACTOR) * normalized;
    }

    /**
     * 这一次挥砍实际活几 tick：配置寿命乘蓄力倍率，不低于 {@link #MIN_LIFETIME_TICKS}。
     */
    public static int lifetimeForCharge(int baseLifetimeTicks, float charge) {
        int base = Math.max(1, baseLifetimeTicks);
        int scaled = Math.round(base * lifetimeFactorForCharge(charge));
        return Math.max(Math.min(MIN_LIFETIME_TICKS, base), scaled);
    }

    /**
     * 单瓣种子只由挥砍种子与序号决定，保证同一挥砍在所有客户端形状一致。
     */
    public static long petalSeed(long swingSeed, int index) {
        long mixed = swingSeed + GOLDEN * (index + 1L);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    /**
     * 由种子取 [0,1) 的稳定随机数；同一输入永远同一输出，不在每帧重新抽取。
     */
    public static float hash01(long seed, int salt) {
        long mixed = seed + GOLDEN * (salt + 1L);
        mixed = (mixed ^ (mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xC4CEB9FE1A85EC53L;
        mixed = mixed ^ (mixed >>> 33);
        return (mixed >>> 40) / (float) (1 << 24);
    }

    /**
     * 第 index 瓣的中心角；旧效果是严格 45° 等分，相位偏移恒为 0。
     */
    public static double petalAngle(int index, int petalCount, float phaseOffset) {
        int count = Math.max(1, petalCount);
        return index * Math.PI * 2.0 / count + phaseOffset;
    }

    public static float clamp01(float value) {
        if (value < 0.0F){
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }

    public static float smoothstep(float edge0, float edge1, float value) {
        if (edge1 <= edge0){
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

    /**
     * 单瓣中心到花心的距离（格）。
     *
     * <p>末帧正好落在 {@code radius} 上，那颗球就是服务端的伤害球，所以画出来的腰环就是它的赤道。
     * 每瓣自己的漂移倍率只影响中途的快慢，不再改变终点：八瓣的末帧中心严格同在一个圆上，
     * 花瓣的松散感留给各自的尺寸倍率（0.8–1.2）。漂移速度被配置成 0 时环也不会缩到花心，
     * 而是从头到尾贴在伤害边界上。
     */
    public static double petalDistance(
            float ageFraction, int lifetimeTicks, float speedPerTick, float speedScale, double radius) {
        if (!(radius > 0.0) || !Double.isFinite(radius)){
            return 0.0;
        }
        double full = ROOT_RADIUS + radialDistance(1.0F, lifetimeTicks, speedPerTick) * speedScale;
        if (full <= DEGENERATE_RING){
            return radius;
        }
        double drifted = ROOT_RADIUS + radialDistance(clamp01(ageFraction), lifetimeTicks, speedPerTick) * speedScale;
        return radius * drifted / full;
    }

    /**
     * 四边形半边：按半径等比缩放，比 {@link #AUTHORED_RING_RADIUS} 大就跟着放大，
     * 于是六格的环不会碎成八道孤立的小刀光，也不会在默认半径下露出多余的空白。
     */
    public static float halfExtentForRadius(double radius, float baseHalfExtent) {
        if (!(radius > 0.0)){
            return 0.0F;
        }
        return (float) (baseHalfExtent * (radius / AUTHORED_RING_RADIUS));
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
        if (t <= fade){
            return 1.0F;
        }
        return 1.0F - smoothstep(fade, 1.0F, t);
    }

    /**
     * 动画帧号；与旧贴图 WITH_AGE 采样一致，按寿命等分切段。
     */
    public static int maskFrame(float ageFraction, int frameCount) {
        int frames = Math.max(1, frameCount);
        int frame = (int) (clamp01(ageFraction) * frames);
        return Math.min(frame, frames - 1);
    }

    private IncursusRiftGeometry() {
    }
}
