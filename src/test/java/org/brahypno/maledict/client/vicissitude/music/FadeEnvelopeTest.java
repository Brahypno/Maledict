package org.brahypno.maledict.client.vicissitude.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 FadeEnvelope 的起始正音量、淡入终点、淡出终点和淡入途中 release 的方向变化。
 * 起始为 0 时，SoundEngine 对不允许静音启动的实例可能跳过播放，包络将没有 tick 机会。
 * 这些算术断言不验证声音注册、OGG 内容或实际播放；音频验收见
 * docs/design/first-vicissitude/04_AUDIO_AND_BOSS_BAR.md。
 */
class FadeEnvelopeTest {

    @Test
    void neverStartsAtZero() {
        FadeEnvelope envelope = new FadeEnvelope(40);
        assertNotEquals(0.0F, envelope.fraction(),
                        "起点为 0 时 SoundEngine#play 会跳过整个播放分支，音乐永远起不来");
        assertTrue(envelope.fraction() > 0.0F, "起点必须是正的");
    }

    @Test
    void fadesInToFullOverTheGivenLength() {
        FadeEnvelope envelope = new FadeEnvelope(40);
        assertTrue(envelope.advance() > 0.0F);
        for (int tick = 1; tick < 40; tick++) {
            envelope.advance();
        }
        assertEquals(1.0F, envelope.fraction(), 1.0E-6F, "淡入结束后应当到满");
        // 再推进也不会越过 1。
        envelope.advance();
        assertEquals(1.0F, envelope.fraction(), 1.0E-6F, "淡入不应当越过满值");
    }

    @Test
    void releaseBringsItBackToSilence() {
        FadeEnvelope envelope = new FadeEnvelope(40);
        for (int tick = 0; tick < 40; tick++) {
            envelope.advance();
        }
        assertFalse(envelope.isReleasing(), "没调 release() 之前不该是淡出状态");
        envelope.release();
        assertTrue(envelope.isReleasing());
        for (int tick = 0; tick < 40; tick++) {
            envelope.advance();
        }
        assertTrue(envelope.isSilent(), "淡出结束后应当是静音");
        assertEquals(0.0F, envelope.fraction(), 1.0E-6F);
    }

    /** 淡入途中 release 应立即转为淡出。 */
    @Test
    void releaseImmediatelyReversesFadeIn() {
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
