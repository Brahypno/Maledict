package org.brahypno.maledict.client;

import org.brahypno.maledict.client.VicissitudeTheme.Participant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 钉住"该不该放 BGM"和"放哪一首"的规则（{@link VicissitudeTheme#select}）。
 *
 * <p>要求是：**Boss 对任何生物都没有仇恨、一阶段都还没开始（{@code DORMANT}）时不放**，
 * 而且**离玩家太远也不放**。两道门缺一不可。
 *
 * <p>这条规则是补出来的，初版只按"离我最近的那只"选，有两个毛病：
 *
 * <ol>
 *   <li><b>沉眠的雕像会把音乐整个压掉。</b> 最近那只如果还在 DORMANT，就返回"不放"，
 *       哪怕二十格外正有一只在一阶段开打。所以不合格的必须被跳过，不能参与"最近"的比较。</li>
 *   <li><b>没有距离门。</b> 扫描半径是 96 格，八十格外别人在打你也会听到 BGM。</li>
 *   <li><b>两只阶段不同时会抖。</b> 玩家在两只之间走动，"最近"来回翻，曲子就来回切；
 *       每次切换都是一次 2 秒交叉淡化，听感直接糊掉。</li>
 * </ol>
 *
 * <p>改成"推进得最远的那只说了算"之后，结果是**单调**的：阶段只前进不后退，
 * 所以同一批无常只会因为真正的阶段推进（或有人死掉、退回沉眠）而换曲。
 *
 * <h2>⚠️ 边界</h2>
 *
 * <p>这里只验**选择规则**。{@code Participant} 是调用方从实体翻译过来的，
 * 所以"哪只算 engaged""{@code isPhaseTwoVisual()} 在客户端读不读得到同步值"
 * "距离怎么量"都不在覆盖范围内——那些只能在游戏里看。
 */
class VicissitudeThemeTest {

    private static Participant dormant() {
        return new Participant(false, true, false);
    }

    /** 合格的一阶段。 */
    private static Participant phaseOne() {
        return new Participant(true, true, false);
    }

    /** 合格的二阶段。 */
    private static Participant phaseTwo() {
        return new Participant(true, true, true);
    }

    /** 在打，但离玩家超过交战半径。 */
    private static Participant farPhaseOne() {
        return new Participant(true, false, false);
    }

    private static Participant farPhaseTwo() {
        return new Participant(true, false, true);
    }

    @Test
    void noBossMeansNoMusic() {
        assertEquals(VicissitudeTheme.NONE, VicissitudeTheme.select(List.of()));
    }

    /** 只有没被唤醒的雕像：不放音乐，否则一进区域就剧透了。 */
    @Test
    void dormantIdolsDoNotPlayAnything() {
        assertEquals(VicissitudeTheme.NONE,
                     VicissitudeTheme.select(List.of(dormant(), dormant())));
    }

    /** 初版就是错在这里：最近的那只是雕像时，把旁边正在打的那只一起压掉了。 */
    @Test
    void dormantIdolDoesNotSuppressAnEngagedNeighbour() {
        assertEquals(VicissitudeTheme.PHASE_ONE,
                     VicissitudeTheme.select(List.of(dormant(), phaseOne())));
    }

    /** 距离门：八十格外的一场战斗不该给你放 BGM，哪怕它真在打。 */
    @Test
    void distantFightsAreSilent() {
        assertEquals(VicissitudeTheme.NONE, VicissitudeTheme.select(List.of(farPhaseOne())));
        assertEquals(VicissitudeTheme.NONE, VicissitudeTheme.select(List.of(farPhaseTwo())));
        assertEquals(VicissitudeTheme.NONE,
                     VicissitudeTheme.select(List.of(farPhaseOne(), farPhaseTwo(), dormant())));
    }

    /** 远处在打、近处也有：只算近的那只，远处的二阶段不该把近处的一阶段顶掉。 */
    @Test
    void distantPhaseTwoDoesNotOverrideNearbyPhaseOne() {
        assertEquals(VicissitudeTheme.PHASE_ONE,
                     VicissitudeTheme.select(List.of(farPhaseTwo(), phaseOne())));
    }

    @Test
    void singleBossFollowsItsOwnPhase() {
        assertEquals(VicissitudeTheme.PHASE_ONE, VicissitudeTheme.select(List.of(phaseOne())));
        assertEquals(VicissitudeTheme.PHASE_TWO, VicissitudeTheme.select(List.of(phaseTwo())));
    }

    /** 多只同阶段：结果不随顺序变，也就不会因为走动而抖。 */
    @Test
    void samePhaseIsOrderIndependent() {
        assertEquals(VicissitudeTheme.PHASE_ONE,
                     VicissitudeTheme.select(List.of(phaseOne(), phaseOne())));
        assertEquals(VicissitudeTheme.PHASE_TWO,
                     VicissitudeTheme.select(List.of(phaseTwo(), phaseTwo())));
    }

    /** 一只先进了二阶段：放二阶段曲，且**和列表顺序无关**——这是"不抖"的关键。 */
    @Test
    void furthestAlongBossWinsRegardlessOfOrder() {
        assertEquals(VicissitudeTheme.PHASE_TWO,
                     VicissitudeTheme.select(List.of(phaseTwo(), phaseOne())));
        assertEquals(VicissitudeTheme.PHASE_TWO,
                     VicissitudeTheme.select(List.of(phaseOne(), phaseTwo())));
        assertEquals(VicissitudeTheme.PHASE_TWO,
                     VicissitudeTheme.select(List.of(phaseOne(), dormant(), phaseTwo(), phaseOne())));
    }

    @Test
    void dormantBossesAreIgnoredEvenAlongsidePhaseTwo() {
        assertEquals(VicissitudeTheme.PHASE_TWO,
                     VicissitudeTheme.select(List.of(dormant(), dormant(), phaseTwo())));
    }
}
