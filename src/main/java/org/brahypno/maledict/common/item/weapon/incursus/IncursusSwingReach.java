package org.brahypno.maledict.common.item.weapon.incursus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 神侵恶刃一次挥砍的伤害球：球心、半径，以及「目标在不在球里」的唯一判法。
 *
 * <p>服务端算一次，命中判定与 {@code IncursusSlashPacket} 发出去的半径读的是同一个数，
 * 所以画出来的腰环与真正能打到的范围不会各说各话：腰环花瓣中心的末帧圆就是这颗球的赤道。
 *
 * <p>球心取挥砍者的腰高。目标按<b>碰撞箱最近点</b>比距离，而不是「脚底点 + 半个箱宽」：
 * 高个 Boss、站在台阶或柱子上的怪、碰撞箱比模型大的实体都按可见体型判，脚底在圈外、身上在圈内的也打得中。
 *
 * <p>半径沿用原版近战判定的 {@code entity_reach + 0.5 + 横扫之刃等级 × 0.25}，但封顶
 * {@link #MAX_RADIUS}：别的模组能把 reach 堆到几十上百格，那样既会扫掉半个区块的实体，
 * 也会把腰环画成一堵墙。非有限值（属性被写成 NaN/Infinity）与负数一律退回 0——打不中，也不画。
 */
public final class IncursusSwingReach {
    /**
     * 原版近战判定的余量。
     */
    public static final double BASE_BONUS = 0.5;
    /**
     * 每级横扫之刃多出的格数。
     */
    public static final double SWEEPING_PER_LEVEL = 0.25;
    /**
     * 半径硬上限。原版是 3.5 格，带一件 +2 reach 的外来饰品也才 5.5 格，6 格盖住所有合法组合。
     */
    public static final double MAX_RADIUS = 6.0;

    private static final double DEGENERATE_RADIUS = 1.0E-6;

    /**
     * 这一次挥砍能打到的半径。非有限或非正的输入一律折成 0，而不是让 NaN 一路传进判定与顶点。
     */
    public static double radiusOf(double entityReach, int sweepingLevel) {
        if (!Double.isFinite(entityReach)){
            return 0.0;
        }
        int sweeping = Math.max(0, sweepingLevel);
        return clampRadius(entityReach + BASE_BONUS + sweeping * SWEEPING_PER_LEVEL);
    }

    /**
     * 半径的最后一道闸：服务端算完、客户端从报文读到，都过这里。
     */
    public static double clampRadius(double raw) {
        if (!Double.isFinite(raw) || raw <= 0.0){
            return 0.0;
        }
        return Math.min(raw, MAX_RADIUS);
    }

    /**
     * 球心：脚底 + 半个身高。
     */
    public static Vec3 centerOf(Entity attacker) {
        Vec3 feet = attacker.position();
        double halfHeight = attacker.getBbHeight() * 0.5;
        return Double.isFinite(halfHeight) ? feet.add(0.0, halfHeight, 0.0) : feet;
    }

    /**
     * 服务端取实体的搜索箱：球的外接立方。任何与球相交的碰撞箱都一定与它相交，
     * 所以这里不会漏掉「中心在球外、身上在球内」的目标。
     */
    public static AABB searchBox(Vec3 center, double radius) {
        return new AABB(center, center).inflate(Math.max(0.0, clampRadius(radius)));
    }

    /**
     * 目标在不在球里：与碰撞箱最近点的距离不大于半径。
     */
    public static boolean touches(AABB targetBox, Vec3 center, double radius) {
        if (!(radius > DEGENERATE_RADIUS) || !isFinite(center) || !isFinite(targetBox)){
            return false;
        }
        return distanceSqrToBox(targetBox, center) <= radius * radius;
    }

    /**
     * 点到一个轴对齐盒子的最短距离平方；落在盒内为 0。
     */
    public static double distanceSqrToBox(AABB box, Vec3 point) {
        double dx = axisGap(point.x, box.minX, box.maxX);
        double dy = axisGap(point.y, box.minY, box.maxY);
        double dz = axisGap(point.z, box.minZ, box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double axisGap(double value, double min, double max) {
        if (value < min){
            return min - value;
        }
        return value > max ? value - max : 0.0;
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean isFinite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
               && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    private IncursusSwingReach() {
    }
}
