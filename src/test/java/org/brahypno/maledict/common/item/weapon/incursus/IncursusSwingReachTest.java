package org.brahypno.maledict.common.item.weapon.incursus;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 神侵恶刃一次挥砍的范围：半径公式、极端值的闸，以及「目标在不在球里」的判法。
 *
 * <p>这些性质必须逐条钉住：半径同时喂命中判定和客户端的腰环，判法又决定了视觉上明明贴着的怪
 * 会不会挨打。改错任何一条，都会退回到「看着打中了却没事」或「画得比打得到处大」的老毛病。
 */
class IncursusSwingReachTest {

    private static final Vec3 WAIST = new Vec3(0.0, 0.9, 0.0);

    @Test
    void radiusKeepsTheVanillaFormulaAndStopsAtTheCap() {
        assertEquals(3.5, IncursusSwingReach.radiusOf(3.0, 0), 1.0E-9, "vanilla reach plus the old 0.5 bonus");
        assertEquals(4.25, IncursusSwingReach.radiusOf(3.0, 3), 1.0E-9, "sweeping edge still adds 0.25 per level");
        assertEquals(2.5, IncursusSwingReach.radiusOf(2.0, -4), 1.0E-9, "a negative level is no bonus");
        assertEquals(IncursusSwingReach.MAX_RADIUS, IncursusSwingReach.radiusOf(50.0, 0), 1.0E-9,
                     "a reach mod must not turn one swing into a half chunk sweep");
        assertEquals(IncursusSwingReach.MAX_RADIUS, IncursusSwingReach.clampRadius(7.0), 1.0E-9);
        assertEquals(3.0, IncursusSwingReach.clampRadius(3.0), 1.0E-9);
    }

    @Test
    void brokenReachHitsNothingInsteadOfEverything() {
        assertEquals(0.0, IncursusSwingReach.radiusOf(Double.NaN, 0), 0.0, "a NaN attribute must not reach the wire");
        assertEquals(0.0, IncursusSwingReach.radiusOf(Double.POSITIVE_INFINITY, 0), 0.0);
        assertEquals(0.0, IncursusSwingReach.radiusOf(Double.NEGATIVE_INFINITY, 0), 0.0);
        assertEquals(0.0, IncursusSwingReach.radiusOf(-10.0, 0), 0.0, "a negative radius is no radius");
        assertEquals(0.0, IncursusSwingReach.clampRadius(Double.NaN), 0.0, "the packet value gets the same gate");
        assertEquals(0.0, IncursusSwingReach.clampRadius(-1.0), 0.0);
        // 半径 0 的挥砍连"站在你身上"的目标都不碰：坏属性不该凭空打人。
        assertFalse(IncursusSwingReach.touches(new AABB(-1.0, -1.0, -1.0, 1.0, 3.0, 1.0), WAIST, 0.0));
        assertFalse(IncursusSwingReach.touches(new AABB(Double.NaN, 0.0, 0.0, 1.0, 1.0, 1.0), WAIST, 3.5),
                    "a non-finite hitbox cannot be argued into reach");
    }

    @Test
    void hitTestUsesTheClosestPointOfTheHitbox() {
        // 站在三格高柱子上的怪：脚底离得远，身子就在圈内。旧判法（脚底点 + 半个箱宽）打不中它。
        AABB perched = new AABB(2.3, 3.0, -0.3, 2.9, 3.85, 0.3);
        assertTrue(IncursusSwingReach.touches(perched, WAIST, 3.5), "an elevated target inside the ring is hit");
        // 巨大碰撞箱的 Boss：脚底中心远在圈外，身体边缘压在圈内。
        AABB wide = new AABB(3.2, 0.0, -2.0, 8.0, 4.0, 2.0);
        assertTrue(IncursusSwingReach.touches(wide, WAIST, 3.5), "a wide hitbox is hit at its nearest edge");
        // 半步之外就该打不中。
        AABB justOutside = new AABB(3.6, 0.0, -0.3, 4.0, 1.8, 0.3);
        assertFalse(IncursusSwingReach.touches(justOutside, WAIST, 3.5), "a target past the radius is missed");
        // 贴脸（球心在箱内）距离为 0。
        assertTrue(IncursusSwingReach.touches(new AABB(-1.0, -1.0, -1.0, 1.0, 3.0, 1.0), WAIST, 3.5));
        // 搜索箱必须罩住所有与球相交的碰撞箱，否则服务端根本捞不到这些目标。
        AABB search = IncursusSwingReach.searchBox(WAIST, 3.5);
        assertTrue(search.intersects(perched.minX, perched.minY, perched.minZ,
                                     perched.maxX, perched.maxY, perched.maxZ),
                   "a hitbox the sphere touches must also intersect the search box");
        assertTrue(search.intersects(wide.minX, wide.minY, wide.minZ, wide.maxX, wide.maxY, wide.maxZ),
                   "a hitbox that only overlaps the sphere at its edge must still be found");
    }
}
