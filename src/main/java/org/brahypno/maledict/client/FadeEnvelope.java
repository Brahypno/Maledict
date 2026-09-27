package org.brahypno.maledict.client;

/**
 * 淡入淡出包络：0..1 的系数，每次推进一拍。
 *
 * <p>抽成不碰 Minecraft 类的纯算术，是为了让"起点不能是 0"这条能被 {@link FadeEnvelopeTest} 直接断言。
 *
 * <p><b>起点必须是 {@code 1/length}，不能是 0。</b>{@code SoundEngine#play} 里有
 * {@code if (calculateVolume(instance.getVolume(), source) == 0.0F && !instance.canStartSilent())}
 * 这么一道门，命中就把**整个播放分支**跳过：声音不创建、不进 {@code tickingSounds}、
 * {@code tick()} 永远不跑。于是淡入进度卡在 0，音量为 0，下一 tick 重试又被跳过——
 * 每 tick 重试、每 tick 被跳过，永远不出声，且默认日志级别下什么都看不到。
 *
 * <p>修法是两道独立保险：这里的起点 1（"几乎听不见但非 0"），以及
 * {@code VicissitudeMusicSound#canStartSilent()} 返回 true。任何一道单独都够用。
 *
 * <p><b>测试覆盖不到的地方，别误以为它管了：</b>{@code FadeEnvelopeTest} 只能证明这个类的算术，
 * 证明不了音乐真的会响。真正卡死过我们的是 {@code canStartSilent()} 那一半，而它需要构造
 * {@code SoundEvent}（触发 {@code Registries} 静态初始化、要 bootstrap），
 * 在纯 JUnit 里做不到——那一半**只能在游戏里验**。见 04 文档 §4 的验收清单。
 */
final class FadeEnvelope {

    private final int length;
    private int progress;
    private boolean releasing;

    FadeEnvelope(int length) {
        this.length = Math.max(1, length);
        this.progress = 1;
    }

    /** 开始淡出。 */
    void release() {
        releasing = true;
    }

    boolean isReleasing() {
        return releasing;
    }

    /** 推进一拍，返回推进后的系数。 */
    float advance() {
        progress = releasing ? Math.max(0, progress - 1) : Math.min(length, progress + 1);
        return fraction();
    }

    /** 不推进，只看当前系数。 */
    float fraction() {
        return progress / (float) length;
    }

    /** 已经淡到静音（只有淡出时才会发生）。 */
    boolean isSilent() {
        return progress <= 0;
    }
}
