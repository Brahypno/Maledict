package org.brahypno.maledict.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.brahypno.maledict.Maledict;

import java.util.Optional;

public final class MaledictNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "main"),
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
        CHANNEL.registerMessage(
                1,
                InfuseSpiritPacket.class,
                InfuseSpiritPacket::encode,
                InfuseSpiritPacket::decode,
                InfuseSpiritPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                2,
                VicissitudeEffectPacket.class,
                VicissitudeEffectPacket::encode,
                VicissitudeEffectPacket::decode,
                VicissitudeEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendRadialAttack() {
        CHANNEL.sendToServer(new RadialAttackPacket());
    }

    public static void sendInfuseSpirit(int containerId, int inventorySlot, ItemStack spiritStack) {
        CHANNEL.sendToServer(new InfuseSpiritPacket(containerId, inventorySlot, spiritStack));
    }

    /** One shot boss presentation event for a single tracking player. */
    public static void sendEffect(ServerPlayer player, VicissitudeEffectPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private MaledictNetwork() {
    }
}
