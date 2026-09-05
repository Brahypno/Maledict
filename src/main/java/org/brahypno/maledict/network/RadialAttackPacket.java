package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.brahypno.maledict.common.combat.IncursusBladeAttack;

import java.util.function.Supplier;

public record RadialAttackPacket() {
    static void encode(RadialAttackPacket packet, FriendlyByteBuf buffer) {
    }

    static RadialAttackPacket decode(FriendlyByteBuf buffer) {
        return new RadialAttackPacket();
    }

    static void handle(RadialAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> IncursusBladeAttack.perform(sender));
        }
        context.setPacketHandled(true);
    }
}
