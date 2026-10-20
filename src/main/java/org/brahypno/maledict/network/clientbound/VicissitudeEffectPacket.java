package org.brahypno.maledict.network.clientbound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vicissitude.vfx.FirstVicissitudeClientEvents;


/**
 * One shot boss event: the server decides that something readable happened and nearby client
 * instances consume it exactly once. Continuous state never travels this way.
 */
public record VicissitudeEffectPacket(int entityId, int eventId, int actionSequence, Vec3 position) implements CustomPacketPayload {
    public static final Type<VicissitudeEffectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VicissitudeEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), VicissitudeEffectPacket::decode);

    @Override
    public Type<VicissitudeEffectPacket> type() { return TYPE; }

    public static final int EVENT_RELEASE = 0;
    public static final int EVENT_TRANSITION_FLASH = 1;
    public static final int EVENT_DEATH_CORE = 2;
    public static final int EVENT_UNSTICK = 3;
    public static final int EVENT_SCYTHE_CATCH = 4;
    public static final int EVENT_HEAVY_IMPACT = 5;
    public static final int EVENT_DEATH_EXTINGUISH = 6;
    public static final int EVENT_SUMMON_OPEN = 7;
    public static final int EVENT_SUMMON_EMERGE = 8;
    public static final int EVENT_SUMMON_ARRIVE = 9;
    public static final int EVENT_SUMMON_CONSUME = 10;

    public static void encode(VicissitudeEffectPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.eventId);
        buffer.writeVarInt(packet.actionSequence);
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
    }

    public static VicissitudeEffectPacket decode(RegistryFriendlyByteBuf buffer) {
        return new VicissitudeEffectPacket(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()));
    }

    public static void handle(VicissitudeEffectPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> FirstVicissitudeClientEvents.accept(packet));
    }
}
