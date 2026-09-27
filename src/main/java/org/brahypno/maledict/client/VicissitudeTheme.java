package org.brahypno.maledict.client;

import java.util.List;

/**
 * 附近的无常加起来，该放哪一首。
 *
 * <p>刻意做成不碰任何 Minecraft 类的纯枚举 + 纯函数，这样 {@link VicissitudeThemeTest} 能直接断言
 * 多 Boss 的选择规则——这条规则踩过坑，见 {@link #select}。
 */
enum VicissitudeTheme {
    /** 没有正在打的无常：不放，或淡出。 */
    NONE,
    PHASE_ONE,
    PHASE_TWO;

    /**
     * 一只无常对选曲的贡献。
     *
     * <p>两道门都得过，缺一不放：
     *
     * <ol>
     *   <li>{@code engaged}——Boss 自己这边：没死、不在垂死、**也不在 {@code DORMANT}**。
     *       {@code DORMANT} 就是"没有任何仇恨、一阶段都还没开始"，那时它只是座雕像，
     *       音乐一响就剧透了。</li>
     *   <li>{@code withinRange}——距离这边：Boss 离本地玩家不超过
     *       {@link VicissitudeBossMusic#COMBAT_RADIUS}。没有这道门的话，八十格外的
     *       一场战斗也会给你放 BGM。</li>
     * </ol>
     *
     * @param engaged     没死、不在垂死、也不是还没被唤醒的雕像
     * @param withinRange 离本地玩家在交战半径内
     * @param phaseTwo    已经进了二阶段（{@code DATA_PHASE_TWO_REACHED}）
     */
    record Participant(boolean engaged, boolean withinRange, boolean phaseTwo) {

        /** 这只到底算不算"我这场战斗里的一只"。 */
        boolean counts() {
            return engaged && withinRange;
        }
    }

    /**
     * 从附近所有无常里挑出该放哪一首。
     *
     * <p><b>多只同时在打时，按"推进得最远"的那只决定。</b> 只要有一只进了二阶段，就放二阶段曲。
     *
     * <p>为什么不是"离我最近的那只"：音乐是**一路全局单轨**，而阶段是**每只各一份**的。
     * 按最近选的话，玩家站在两只之间走动时"最近"会来回翻，两只阶段不同就会不停切歌——
     * 每次切换都是一次 2 秒交叉淡化，听感直接糊掉。
     * 按"最远推进"选则是**单调**的：阶段只前进不后退，所以同一批无常的结果只会在
     * 真正发生阶段推进（或有 Boss 死掉/退回沉眠）时才变，而那正是我们想让它变的时候。
     *
     * <p>代价是放弃"跟着我在打的那只走"。两只阶段不同、而你正贴着落后那只打时，
     * 听到的会是先进那只的曲子——这是有意接受的取舍，换来的是绝不抖。
     *
     * <p><b>不满足 {@link Participant#counts()} 的一律跳过，不参与任何比较。</b>
     * 这不是可选的：初版只按距离选、且没排除 {@code DORMANT}，最近那只如果还是座雕像，
     * 就会返回"不放音乐"，哪怕二十格外正有一只在一阶段开打。
     */
    static VicissitudeTheme select(List<Participant> bosses) {
        boolean anyCounting = false;
        for (Participant boss : bosses) {
            if (!boss.counts()) {
                continue;
            }
            anyCounting = true;
            if (boss.phaseTwo()) {
                return PHASE_TWO;
            }
        }
        return anyCounting ? PHASE_ONE : NONE;
    }
}
