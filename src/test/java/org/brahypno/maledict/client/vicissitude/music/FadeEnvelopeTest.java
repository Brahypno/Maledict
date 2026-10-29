package org.brahypno.maledict.client.vicissitude.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 钉住 {@link FadeEnvelope} 的算术，特别是"淡入系数不能从 0 开始"。
 *
 * <p>{@code SoundEngine#play} 里有这道门：
 *
 * <pre>
 * float f2 = this.calculateVolume(instance.getVolume(), instance.getSource());
 * if (f2 == 0.0F &amp;&amp; !instance.canStartSilent()) {
 *     // 只打一条 debug 日志，**整个播放分支都不走**
 * } else {
 *     ... 创建 channel、instanceToChannel.put、tickingSounds.add ...
 * }
 * </pre>
 *
 * <p>一旦被跳过，声音就不在 {@code tickingSounds} 里，{@code tick()} 永远不会被调用，
 * 淡入进度卡在 0，音量永远是 0 —— 下一次 {@code play()} 又被跳过。每 tick 重试、每 tick 被跳过，
 * <b>永远不出声</b>，而且默认日志级别下什么都看不到。
 *
 * <h2>⚠️ 这个测试的边界，别当成"音乐能响"的证明</h2>
 *
 * <p>踩过的那次坑里，光靠这里的断言是**抓不到**的：
 *
 * <ul>
 *   <li>它只覆盖两道保险里的**一道**（起点非 0）。另一道是
 *       {@code VicissitudeMusicSound#canStartSilent()} 返回 {@code true}，那需要构造
 *       {@code SoundEvent}——会触发 {@code Registries} 静态初始化、要求 bootstrap，
 *       而 {@code Bootstrap.bootStrap()} 在纯 JUnit 环境里会炸在 Forge 的
 *       {@code NetworkHooks} 上（试过，{@code initializationError}）。
 *       所以**那一半只能在游戏里验**。</li>
 *   <li>它证明不了事件 id、{@code sounds.json}、.ogg 文件、注册表接线任何一环是对的。
 *       那些环节任一出错，同样是一声不响。</li>
 * </ul>
 *
 * <p>换句话说：这个类是**为了让这条不变量可测**才从 {@code VicissitudeMusicSound} 里抽出来的，
 * 它管算术，不管播放。真要确认音乐响了，只有 {@code runClient} 一途，
 * 清单见 {@code docs/design/first-vicissitude/04_AUDIO_AND_BOSS_BAR.md} §4。
 */
class FadeEnvelopeTest {

    /** 淡出途中改主意再淡入，也要能回到满——切歌时旧实例淡出、新实例淡入用的就是这条路径。 */
    @Test
    void releasesAndRisesAgain() {
        FadeEnvelope envelope = new FadeEnvelope(40);
        for (int tick = 0; tick < 20; tick++) {
            envelope.advance();
        }
        float midway = envelope.fraction();
        envelope.release();
        envelope.advance();
        assertTrue(envelope.fraction() < midway, "release 之后应当往回走");
    }
}
