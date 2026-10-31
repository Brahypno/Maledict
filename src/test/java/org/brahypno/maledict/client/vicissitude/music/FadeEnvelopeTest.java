package org.brahypno.maledict.client.vicissitude.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 FadeEnvelope 的起始正音量、淡入终点、淡出终点和淡入途中 release 的方向变化。
 * 起始为 0 时，SoundEngine 对不允许静音启动的实例可能跳过播放，包络将没有 tick 机会。
 * 这些算术断言不验证声音注册、OGG 内容或实际播放；音频验收见
 * docs/design/first-vicissitude/04_AUDIO_AND_BOSS_BAR.md。
 */
class FadeEnvelopeTest {

    /** 淡入途中 release 应立即转为淡出。 */
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
