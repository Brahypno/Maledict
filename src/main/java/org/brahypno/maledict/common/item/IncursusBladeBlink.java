package org.brahypno.maledict.common.item;

/**
 * 神侵恶刃在物品栏里的眨眼规律。
 *
 * <p>物品栏用的贴图只有两张——睁眼和闭眼——所谓「眨眼」就是偶尔把模型换成闭眼那张，
 * 几个 tick 之后换回来（换贴图本身由 {@code MaledictItemModels} 里的模型覆盖完成，
 * 这里只管「这一 tick 该不该闭眼」）。三档注视状态各有一套节奏：
 *
 * <ul>
 *   <li>{@link Gaze#SELECTED}：正躺在快捷栏选中的那一格，也就是拿在手上的那把。最紧张，
 *       平均约 2.3 秒眨一下，偶尔还连眨两下。</li>
 *   <li>{@link Gaze#HOVERED}：鼠标正指着它（悬停的格子 / 鼠标上拿着的那一份）。被盯着看，
 *       平均约 5 秒一下。</li>
 *   <li>{@link Gaze#IDLE}：只是躺在背包里没人理。最闲，平均约 11 秒一下，而且有三成半轮次
 *       干脆不眨——走神了。</li>
 * </ul>
 *
 * <p><b>随机，但不记状态。</b>时间轴按 {@code cycleTicks} 切成一轮一轮，每一轮的闭眼时刻、
 * 闭眼时长、要不要连眨、要不要整轮跳过，全部从 {@code (种子, 轮次)} 哈希出来。于是：
 * 同一个堆在同一个 tick 永远得到同一个答案（一秒渲染六十次也不会抖），但看上去毫无节奏；
 * 不同的堆种子不同，各眨各的，不会像复制粘贴出来的一排。
 *
 * <p>时钟是单调的客户端 tick，所以「第几轮」只跟时间有关，跟这一帧渲染的是背包、快捷栏
 * 还是创造模式列表无关——同一把刃在哪儿看都是同一个它。
 */
public final class IncursusBladeBlink {

    /** 这一帧这把刃和玩家的关系，决定用哪一档节奏。 */
    public enum Gaze {
        /** 快捷栏选中那一格里的那把（拿在手上的那把）。 */
        SELECTED,
        /** 鼠标指着它，或者鼠标上正拿着它。 */
        HOVERED,
        /** 仅仅躺在物品栏里。 */
        IDLE
    }

    /**
     * 单次闭眼的时长（tick）。
     *
     * <p>真人眨眼大约 100–150 毫秒，也就是 2–3 tick；这里给 1–3，短的那一下看起来更像
     * 「眼睛一眯」而不是「关灯」。（测试直接读这两个数当断言，所以是包内可见。）
     */
    static final int MIN_BLINK_TICKS = 1;
    static final int MAX_BLINK_TICKS = 3;

    /** 连眨两下时，中间睁眼的那道缝。太短会糊成一次长闭眼。 */
    private static final int DOUBLE_BLINK_GAP_MIN = 3;
    private static final int DOUBLE_BLINK_GAP_MAX = 5;

    /**
     * 一轮的两头各留多少 tick 的睁眼余量。
     *
     * <p>不留的话，上一轮结尾的那次闭眼会和下一轮开头的那次挨在一起，看起来不是「眨了两下」
     * 而是「闭了很久」；而且跨轮的闭眼会连成一段，把「单次闭眼不超过 {@link #MAX_BLINK_TICKS}
     * tick」这条规律也破坏掉（{@code IncursusBladeBlinkTest} 正是拿它当断言）。
     */
    private static final int CYCLE_EDGE_TICKS = 2;

    /**
     * 一档节奏。
     *
     * <p>频率别调高：真人一分钟才眨十五到二十下（平均四五秒一次），物品比人眨得勤就会显得
     * 抽搐而不是「活着」。三档现在平均分别是两三秒、五秒、十来秒一次。
     *
     * @param cycleTicks         多少 tick 算一轮
     * @param skipPercent        这一轮干脆不眨的概率（走神）
     * @param doubleBlinkPercent 这一轮连眨两下的概率
     */
    private record Pattern(int cycleTicks, int skipPercent, int doubleBlinkPercent) {
    }

    private static final Pattern SELECTED_PATTERN = new Pattern(36, 20, 20);
    private static final Pattern HOVERED_PATTERN = new Pattern(72, 25, 10);
    private static final Pattern IDLE_PATTERN = new Pattern(140, 35, 0);

    /**
     * 这一 tick 要不要闭眼。
     *
     * @param seed 这个堆自己的种子，见 {@link #seedOf(Object)}
     * @param gaze 注视状态
     * @param tick 客户端 tick（单调递增即可）
     */
    public static boolean isClosed(int seed, Gaze gaze, long tick) {
        Pattern pattern = patternOf(gaze);
        int cycleTicks = pattern.cycleTicks();
        long cycle = Math.floorDiv(tick, cycleTicks);
        int phase = (int) Math.floorMod(tick, cycleTicks);

        long hash = mix(seed * 0x9E3779B97F4A7C15L + cycle);
        if (roll(hash, 100) < pattern.skipPercent()) {
            return false;
        }

        hash = mix(hash);
        int blinkTicks = MIN_BLINK_TICKS + roll(hash, MAX_BLINK_TICKS - MIN_BLINK_TICKS + 1);

        hash = mix(hash);
        boolean doubleBlink = roll(hash, 100) < pattern.doubleBlinkPercent();

        hash = mix(hash);
        int gap = doubleBlink
                ? DOUBLE_BLINK_GAP_MIN + roll(hash, DOUBLE_BLINK_GAP_MAX - DOUBLE_BLINK_GAP_MIN + 1)
                : 0;

        // 整段（一次闭眼，或者「闭—睁—闭」）在这一轮里找个起点。两头各留 CYCLE_EDGE_TICKS：
        // 不跨轮，上一轮结尾的闭眼就不会和这一轮开头的闭眼连成一段。
        hash = mix(hash);
        int blockTicks = doubleBlink ? blinkTicks * 2 + gap : blinkTicks;
        int start = CYCLE_EDGE_TICKS
                + roll(hash, cycleTicks - blockTicks - CYCLE_EDGE_TICKS * 2 + 1);

        if (phase >= start && phase < start + blinkTicks) {
            return true;
        }
        return doubleBlink && phase >= start + blinkTicks + gap && phase < start + blockTicks;
    }

    /** 渲染谓词用的那一层皮：{@code 1.0} 闭眼、{@code 0.0} 睁眼。 */
    public static float blinkValue(int seed, Gaze gaze, long tick) {
        return isClosed(seed, gaze, tick) ? 1.0F : 0.0F;
    }

    /**
     * 一个堆的随机种子。
     *
     * <p>用<b>对象身份</b>而不是内容哈希：用内容的话，背包里两把一模一样的刃会眨得完全同步，
     * 一看就是复制出来的；身份哈希让每一个 {@code ItemStack} 实例各眨各的，而实例在背包里
     * 是稳定的，所以同一把刃的节奏也不会一帧一变。
     */
    public static int seedOf(Object stack) {
        return System.identityHashCode(stack);
    }

    private static Pattern patternOf(Gaze gaze) {
        return switch (gaze) {
            case SELECTED -> SELECTED_PATTERN;
            case HOVERED -> HOVERED_PATTERN;
            case IDLE -> IDLE_PATTERN;
        };
    }

    /** 从哈希里取一个 {@code [0, bound)} 的整数。 */
    private static int roll(long hash, int bound) {
        return (int) Math.floorMod(hash >>> 17, (long) bound);
    }

    /** splitmix64 的收尾混合：把相邻的种子/轮次彻底打散。 */
    private static long mix(long value) {
        long mixed = value + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private IncursusBladeBlink() {
    }
}
