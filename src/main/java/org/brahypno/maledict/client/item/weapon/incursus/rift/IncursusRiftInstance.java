package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;

import java.util.List;

/**
 * 一次裂隙效果。
 *
 * <p>两种形态，刻意分开保存：
 * <ul>
 *   <li><b>普通挥砍</b>走旧的八瓣模型（{@link IncursusRiftPetal}）：花心跟着挥砍者走，
 *       贴图帧、透明度与开合都按<b>整个实例</b>求值。{@code radius} 是服务端这一次挥砍的伤害半径，
 *       花瓣中心末帧圆落在它上面，所以画的圈就是真正能打到的圈；{@code charge} 只压薄厚与寿命。</li>
 *   <li><b>飞腾与隐藏锋刃</b>走逐道刀痕（{@link RiftStroke}）：几百道各自错峰出生，
 *       锚点固定在触发时的世界坐标。</li>
 * </ul>
 *
 * <p>两者都只保存创建时固定的参数；顶点每帧由 {@code age + partialTick} 重新求值，
 * 不做逐帧位移累加，避免帧率不同导致形状漂移。
 */
public final class IncursusRiftInstance {

    private static final IncursusRiftPetal[] NO_PETALS = new IncursusRiftPetal[0];
    private static final RiftStroke[] NO_STROKES = new RiftStroke[0];
    /**
     * 报文来自网络，非法值不该让一个实例永远活着；四种布局的实际上限是 24 tick。
     */
    private static final float MAX_SKILL_LIFETIME_TICKS = 200.0F;

    private final RiftMode mode;
    private final RiftAnchorMode anchorMode;
    private final int ownerId;
    private final Vec3 center;
    private final double radius;
    private final float charge;
    private final long seed;
    private final IncursusRiftPetal[] petals;
    private final RiftStroke[] strokes;
    private final int lifetimeTicks;
    private final float fadeStart;
    private int age;

    private IncursusRiftInstance(
            RiftMode mode, RiftAnchorMode anchorMode, int ownerId, Vec3 center, double radius, float charge,
            long seed, IncursusRiftPetal[] petals, RiftStroke[] strokes, int lifetimeTicks, float fadeStart) {
        if (!Double.isFinite(radius) || radius < 0.0){
            throw new IllegalArgumentException("Invalid swing radius: " + radius);
        }
        if (!Float.isFinite(charge) || charge < 0.0F || charge > 1.0F){
            throw new IllegalArgumentException("Invalid swing charge: " + charge);
        }
        this.mode = mode;
        this.anchorMode = anchorMode;
        this.ownerId = ownerId;
        this.center = center;
        this.radius = radius;
        this.charge = charge;
        this.seed = seed;
        this.petals = petals;
        this.strokes = strokes;
        this.lifetimeTicks = lifetimeTicks;
        this.fadeStart = fadeStart;
    }

    /**
     * 普通挥砍：八瓣水平腰环。花瓣参数与旧实现的 {@code create} 一一对应，蓄力换成了服务端算出的
     * 伤害半径（决定大小）与攻击强度（决定薄厚与收尾）。
     */
    public static IncursusRiftInstance createMelee(
            int ownerId, Vec3 center, double radius, float charge, long seed,
            int lifetimeTicks, int petalCount, float fadeStart) {
        int count = Math.max(1, petalCount);
        IncursusRiftPetal[] petals = new IncursusRiftPetal[count];
        for (int i = 0; i < count; i++) {
            petals[i] = IncursusRiftPetal.derive(seed, i, count);
        }
        float normalized = IncursusRiftGeometry.normalizeCharge(charge);
        return new IncursusRiftInstance(
                RiftMode.MELEE_RING, RiftAnchorMode.FOLLOW_OWNER, ownerId, center, radius, normalized, seed,
                petals, NO_STROKES,
                IncursusRiftGeometry.lifetimeForCharge(lifetimeTicks, normalized), fadeStart);
    }

    /**
     * 技能模式：一批已经算好的刀痕，锚点固定在触发时的世界坐标。
     *
     * <p>它们按刀痕画，不读半径与蓄力，所以 {@code radius} 记 0 表示"这次效果没有腰环"，
     * 蓄力记 1 表示不额外压淡。
     */
    public static IncursusRiftInstance createSkill(
            RiftMode mode, int ownerId, Vec3 center, long seed, List<RiftStroke> strokes) {
        if (mode == RiftMode.MELEE_RING){
            throw new IllegalArgumentException("Use createMelee for the waist ring");
        }
        RiftStroke[] array = strokes.toArray(new RiftStroke[0]);
        float lastTick = 0.0F;
        for (RiftStroke stroke : array) {
            lastTick = Math.max(lastTick, stroke.endTick());
        }
        int lifetime = Math.min((int) Math.ceil(Math.max(lastTick, 0.0F)), (int) MAX_SKILL_LIFETIME_TICKS);
        IncursusRiftInstance instance = new IncursusRiftInstance(
                mode, RiftAnchorMode.FIXED_WORLD, ownerId, center, 0.0, 1.0F, seed,
                NO_PETALS, array, Math.max(1, lifetime + 1), 0.95F);
        // The particle engine adds a newly spawned particle after ticking the live particles.
        instance.age = -1;
        return instance;
    }

    /**
     * 前进一个客户端 tick。
     */
    public void tick() {
        age++;
    }

    public boolean isExpired() {
        return age >= lifetimeTicks;
    }

    public float ageFraction(float partialTick) {
        float fraction = (age + partialTick) / lifetimeTicks;
        return IncursusRiftGeometry.clamp01(fraction);
    }

    public float alpha(float partialTick) {
        return IncursusRiftGeometry.alpha(ageFraction(partialTick), fadeStart)
               * IncursusRiftGeometry.opacityForCharge(charge);
    }

    public int maskFrame(float partialTick) {
        return IncursusRiftGeometry.maskFrame(ageFraction(partialTick), IncursusRiftGeometry.MASK_FRAMES);
    }

    public int ownerId() {
        return ownerId;
    }

    /**
     * 触发时的世界坐标；跟随模式下只作实体缺席时的兜底锚点。
     */
    public Vec3 center() {
        return center;
    }

    public RiftMode mode() {
        return mode;
    }

    public RiftAnchorMode anchorMode() {
        return anchorMode;
    }

    /**
     * 这一次挥砍的伤害半径（普通挥砍专用；技能模式为 0，它们按刀痕画）。
     */
    public double radius() {
        return radius;
    }

    /**
     * 服务端结算这一刀时的攻击强度（0..1）：只压薄厚与收尾，不参与半径。
     */
    public float charge() {
        return charge;
    }

    public long seed() {
        return seed;
    }

    public IncursusRiftPetal[] petals() {
        return petals;
    }

    public RiftStroke[] strokes() {
        return strokes;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public float fadeStart() {
        return fadeStart;
    }

    public int age() {
        return age;
    }
}
