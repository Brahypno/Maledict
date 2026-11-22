package org.brahypno.maledict.compat.maid.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.CustomSoundLoader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.MaidSoundInstanceAtPos;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.SoundCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.brahypno.maledict.network.clientbound.MaidVoicePacket;

/**
 * Client side of {@link MaidVoicePacket}. 只有女仆模组在场时才会被调用。
 *
 * <p>1.20.1 这里要新建一只「临时客户端女仆」当坐标载体，因为 TLM 1.20.1 的
 * {@code MaidSoundInstance} 只收实体、不收坐标。1.21 的 TLM 补上了
 * {@link MaidSoundInstanceAtPos}（{@code (SoundEvent, soundPackId, x, y, z, volume, pitch)}），
 * 效果与 {@code ICustomSoundBuffer} 的自定义音源完全一致，所以那只假女仆可以删掉——它本来就会在
 * 客户端世界里留下一个真实的 {@code EntityMaid} 实例。
 */
@OnlyIn(Dist.CLIENT)
public final class MaledictMaidVoiceClient {
    public static void play(MaidVoicePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(packet.sound());
        if (sound == null)
            return;
        SoundCache cache = CustomSoundLoader.getSoundCache(packet.soundPack());
        var buffers = cache == null ? null : cache.buffers().get(packet.sound());
        if (buffers == null || buffers.isEmpty()){
            // TLM's file-less sound event produces a subtitle but no audio; use a real vanilla
            // hurt sound when the selected voice pack has no usable buffer for this event.
            minecraft.level.playLocalSound(packet.x(), packet.y(), packet.z(), SoundEvents.PLAYER_HURT,
                                           SoundSource.VOICE, 1.0F, 1.0F, false);
            return;
        }

        minecraft.getSoundManager().play(
                new MaidSoundInstanceAtPos(sound, packet.soundPack(), packet.x(), packet.y(), packet.z(),
                                           1.0F, 1.0F));
    }

    private MaledictMaidVoiceClient() {
    }
}
