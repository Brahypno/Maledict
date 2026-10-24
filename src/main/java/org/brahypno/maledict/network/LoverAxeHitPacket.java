package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.client.weapon.lover.LoverAxeHitEffects;

import java.util.function.Supplier;

/** A position snapshot also presents lethal hits whose target has already left the client world. */
public record LoverAxeHitPacket(int targetId, Vec3 center, float radius) {
    public static void encode(LoverAxeHitPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetId);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.radius);
    }

    public static LoverAxeHitPacket decode(FriendlyByteBuf buffer) {
        return new LoverAxeHitPacket(buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
    }

    public static void handle(LoverAxeHitPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> LoverAxeHitEffects.spawn(packet)));
        context.setPacketHandled(true);
    }
}
