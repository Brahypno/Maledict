package org.brahypno.maledict.compat.maid.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.CustomSoundLoader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.MaidSoundInstance;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.network.MaidVoicePacket;

/**
 * Client side of {@link MaidVoicePacket}. TLM's voice-pack sound instance wants a maid for position,
 * so a transient, non-added client maid is used as a pure position carrier.
 */
@OnlyIn(Dist.CLIENT)
public final class MaledictMaidVoiceClient {
    public static void play(MaidVoicePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(packet.sound());
        if (sound == null)
            return;
        var cache = CustomSoundLoader.getSoundCache(packet.soundPack());
        var buffers = cache == null ? null : cache.buffers().get(packet.sound());
        if (buffers == null || buffers.isEmpty()){
            // TLM's file-less sound event produces a subtitle but no audio; use a real vanilla
            // hurt sound when the selected voice pack has no usable buffer for this event.
            minecraft.level.playLocalSound(packet.x(), packet.y(), packet.z(), SoundEvents.PLAYER_HURT,
                                           SoundSource.VOICE, 1.0F, 1.0F, false);
            return;
        }

        EntityMaid carrier = new EntityMaid(minecraft.level);
        carrier.setPos(packet.x(), packet.y(), packet.z());
        carrier.setSoundPackId(packet.soundPack());
        minecraft.getSoundManager().play(new MaidSoundInstance(sound, packet.soundPack(), carrier));
    }

    private MaledictMaidVoiceClient() {
    }
}
