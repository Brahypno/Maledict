package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.world.phys.Vec3;

/**
 * 一次挥砍对应的一朵裂隙花。
 *
 * <p>只保存创建时固定的参数与年龄；所有顶点每帧由 {@code age + partialTick} 重新求值，
 * 不做逐帧位移累加，避免帧率不同导致形状漂移。
 */
public final class IncursusRiftInstance {
    private final Vec3 center;
    private final float scale;
    private final long seed;
    private final IncursusRiftPetal[] petals;
    private final int lifetimeTicks;
    private final float fadeStart;
    private int age;

    private IncursusRiftInstance(
            Vec3 center, float scale, long seed, IncursusRiftPetal[] petals,
            int lifetimeTicks, float fadeStart) {
        this.center = center;
        this.scale = scale;
        this.seed = seed;
        this.petals = petals;
        this.lifetimeTicks = Math.max(1, lifetimeTicks);
        this.fadeStart = fadeStart;
    }

    public static IncursusRiftInstance create(
            Vec3 center, float scale, long seed, int lifetimeTicks, int petalCount, float fadeStart) {
        int count = Math.max(1, petalCount);
        IncursusRiftPetal[] petals = new IncursusRiftPetal[count];
        for (int i = 0; i < count; i++) {
            petals[i] = IncursusRiftPetal.derive(seed, i, count);
        }
        return new IncursusRiftInstance(center, scale, seed, petals, lifetimeTicks, fadeStart);
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
        return IncursusRiftGeometry.alpha(ageFraction(partialTick), fadeStart);
    }

    public int maskFrame(float partialTick) {
        return IncursusRiftGeometry.maskFrame(ageFraction(partialTick), IncursusRiftGeometry.MASK_FRAMES);
    }

    public Vec3 center() {
        return center;
    }

    public float scale() {
        return scale;
    }

    public long seed() {
        return seed;
    }

    public IncursusRiftPetal[] petals() {
        return petals;
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
