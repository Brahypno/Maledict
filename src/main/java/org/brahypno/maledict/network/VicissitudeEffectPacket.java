package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.vfx.FirstVicissitudeClientEvents;

import java.util.function.Supplier;

/**
 * One shot boss event: the server decides that something readable happened and nearby client
 * instances consume it exactly once. Continuous state never travels this way.
 */
public record VicissitudeEffectPacket(int entityId, int eventId, int actionSequence, Vec3 position) {
    public static final int EVENT_RELEASE = 0;
    public static final int EVENT_TRANSITION_FLASH = 1;
    public static final int EVENT_DEATH_CORE = 2;
    public static final int EVENT_UNSTICK = 3;
    public static final int EVENT_SCYTHE_CATCH = 4;
    public static final int EVENT_HEAVY_IMPACT = 5;
    public static final int EVENT_DEATH_EXTINGUISH = 6;

    public static void encode(VicissitudeEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.eventId);
        buffer.writeVarInt(packet.actionSequence);
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
    }

    public static VicissitudeEffectPacket decode(FriendlyByteBuf buffer) {
        return new VicissitudeEffectPacket(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()));
    }

    public static void handle(VicissitudeEffectPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FirstVicissitudeClientEvents.accept(packet)));
        context.setPacketHandled(true);
    }
}
