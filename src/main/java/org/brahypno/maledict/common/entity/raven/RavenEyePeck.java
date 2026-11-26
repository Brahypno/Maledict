package org.brahypno.maledict.common.entity.raven;

/**
 * 渡鸦取眼的判定：起手距离、放弃距离、下嘴距离，加上「这名玩家该不该挨」的四项否决。
 *
 * <p>和 {@link RavenShotSafety} 一样只做纯函数，实体负责把事实取出来：成年、仪式印记、信任关系、
 * 玩家能不能挨打（创造与旁观不啄，与 {@code RavenEntity#canAttack} 同一口径），以及距离和视线。
 * 判定集中在这里，是为了让「谁会被啄」只有一处口径，也便于单测钉住半径。
 *
 * <p>啄完之后仪式那边的印记会被摘掉，同一轮献祭不会有第二次，所以这里不需要额外的冷却。
 */
public final class RavenEyePeck {
    /**
     * 起手距离：八格。看见就动身，不需要先把他当攻击目标。
     */
    public static final double SIGHT_RADIUS = 8.0D;

    /**
     * 放弃距离：起手八格，他跑远了追到十六格还没够到就放掉。
     */
    public static final double ABANDON_RADIUS = 16.0D;

    /**
     * 下嘴距离：鸦眼到人眼 1.6 格。得贴着脑袋才够得着眼睛，站在脚边不算。
     */
    public static final double REACH = 1.6D;

    /**
     * 起手：看得见的献祭者、八格以内，且四项否决全过。
     */
    public static boolean maySeek(boolean adult, boolean markedByRite, boolean trusted, boolean mayBeHarmed,
                                  double distanceSqr, boolean visible) {
        return mayContinue(adult, markedByRite, trusted, mayBeHarmed, distanceSqr)
               && visible && distanceSqr <= SIGHT_RADIUS * SIGHT_RADIUS;
    }

    /**
     * 追下去：起手后不必每 tick 都看得见，跑进十六格以外才算跟丢。
     */
    public static boolean mayContinue(boolean adult, boolean markedByRite, boolean trusted, boolean mayBeHarmed,
                                      double distanceSqr) {
        return adult && markedByRite && !trusted && mayBeHarmed
               && distanceSqr <= ABANDON_RADIUS * ABANDON_RADIUS;
    }

    /**
     * 够到眼睛了没有；参数是鸦眼到人眼的距离平方。
     */
    public static boolean inReach(double eyeDistanceSqr) {
        return eyeDistanceSqr <= REACH * REACH;
    }

    private RavenEyePeck() {
    }
}
