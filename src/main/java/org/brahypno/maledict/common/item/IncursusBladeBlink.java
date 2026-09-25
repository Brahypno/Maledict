package org.brahypno.maledict.common.item;

/**
 * 神侵恶刃在物品栏里的眨眼规律：按 {@code cycleTicks} 切轮，每轮该不该闭眼由 {@code (种子, 轮次)} 哈希出来。
 */
public final class IncursusBladeBlink {

    /** 这一帧这把刃和玩家的关系，决定用哪一档节奏。 */
    public enum Gaze {
        SELECTED,
        HOVERED,
        IDLE
    }

    /** 单次闭眼的时长（tick）；测试直接读这两个数，所以是包内可见。 */
    static final int MIN_BLINK_TICKS = 1;
    static final int MAX_BLINK_TICKS = 3;

    /** 连眨两下时，中间睁眼的那道缝（tick）；太短会糊成一次长闭眼。 */
    private static final int DOUBLE_BLINK_GAP_MIN = 3;
    private static final int DOUBLE_BLINK_GAP_MAX = 5;

    /** 一轮的两头各留多少 tick 的睁眼余量，不留的话跨轮的闭眼会连成一段。 */
    private static final int CYCLE_EDGE_TICKS = 2;

    private record Pattern(int cycleTicks, int skipPercent, int doubleBlinkPercent) {
    }

    private static final Pattern SELECTED_PATTERN = new Pattern(36, 20, 20);
    private static final Pattern HOVERED_PATTERN = new Pattern(72, 25, 10);
    private static final Pattern IDLE_PATTERN = new Pattern(140, 35, 0);

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

        // 整段（一次闭眼，或者「闭—睁—闭」）在本轮内找起点，两头各留 CYCLE_EDGE_TICKS：不跨轮。
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
     * 一个堆的随机种子：用对象身份而不是内容哈希，否则两把一模一样的刃会眨得完全同步。
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

    private static int roll(long hash, int bound) {
        return (int) Math.floorMod(hash >>> 17, (long) bound);
    }

    /** splitmix64 的收尾混合。 */
    private static long mix(long value) {
        long mixed = value + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private IncursusBladeBlink() {
    }
}
