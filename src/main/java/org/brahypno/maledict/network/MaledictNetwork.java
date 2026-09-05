package org.brahypno.maledict.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.brahypno.maledict.Maledict;

import java.util.Optional;

public final class MaledictNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Maledict.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(
                0,
                RadialAttackPacket.class,
                RadialAttackPacket::encode,
                RadialAttackPacket::decode,
                RadialAttackPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendRadialAttack() {
        CHANNEL.sendToServer(new RadialAttackPacket());
    }

    private MaledictNetwork() {
    }
}
