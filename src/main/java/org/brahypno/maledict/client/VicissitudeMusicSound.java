package org.brahypno.maledict.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 无常 Boss 战的一首背景乐，自带淡入淡出。
 *
 * <p>几个必须这么写的点，改之前先看：
 *
 * <ul>
 *   <li><b>{@code delay} 必须保持 0。</b>{@code SoundEngine} 用
 *       {@code requiresManualLooping = getDelay() > 0} 分流，只有 {@code looping && delay == 0}
 *       才走 {@code shouldLoopAutomatically}，也就是 OpenAL 的硬件循环。非 0 会变成"手动循环"，
 *       接缝会很难听。</li>
 *   <li><b>音源是 {@link SoundSource#MUSIC}。</b> 这样玩家把「音乐」滑条拉到 0 就真的静音，
 *       符合预期；{@code SoundEngine} 会在音量为 0 时直接跳过播放，不用自己判断。</li>
 *   <li><b>{@code attenuation = NONE} + {@code relative = true}。</b> 音乐不做空间定位，
 *       也不随距离衰减。真正决定"听不听得见"的是阶段映射，见 {@link VicissitudeBossMusic}。</li>
 *   <li><b>{@code volume} 是乘数</b>，最终音量 = {@code volume × sound.volume × 音乐滑条}。</li>
 *   <li><b>{@link #canStartSilent()} 必须返回 true</b>，且 {@link FadeEnvelope} 的起点必须非 0。
 *       两道保险都别删，原因写在 {@link FadeEnvelope} 的类注释里——删掉音乐就整个哑掉。</li>
 * </ul>
 */
final class VicissitudeMusicSound extends AbstractTickableSoundInstance {

    /** 淡入淡出各占多少 tick。40 tick = 2 秒。 */
    static final int FADE_TICKS = 40;

    private final SoundEvent soundEvent;
    private final float targetVolume;
    private final FadeEnvelope envelope = new FadeEnvelope(FADE_TICKS);

    VicissitudeMusicSound(SoundEvent soundEvent, boolean looping, float targetVolume) {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.soundEvent = soundEvent;
        this.targetVolume = targetVolume;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.looping = looping;
        // 必须保持 0：见类注释。
        this.delay = 0;
        this.volume = targetVolume * envelope.fraction();
    }

    /** 允许在音量为 0 时就开始播放，否则淡入永远起不来。 */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    SoundEvent getSoundEvent() {
        return soundEvent;
    }

    /**
     * 当前淡入淡出算出来的乘数。
     *
     * <p>不能用继承来的 {@code getVolume()}：那个会乘上 {@code sound.getVolume()}，
     * 而 {@code sound} 字段要等 {@code resolve()} 之后才非 null。
     */
    float fadeMultiplier() {
        return volume;
    }

    /** 开始淡出。实例会在淡到静音后自己 {@code stop()}，调用方不用管它的后续。 */
    void fadeOut() {
        envelope.release();
    }

    boolean isFadingOut() {
        return envelope.isReleasing();
    }

    @Override
    public void tick() {
        volume = targetVolume * envelope.advance();
        if (envelope.isReleasing() && envelope.isSilent()) {
            stop();
        }
    }
}
