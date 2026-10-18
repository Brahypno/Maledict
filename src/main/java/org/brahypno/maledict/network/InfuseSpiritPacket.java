package org.brahypno.maledict.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.brahypno.maledict.Maledict;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import org.brahypno.maledict.common.item.IncursusBladeItem;


public record InfuseSpiritPacket(int containerId, int inventorySlot, ItemStack spiritStack) implements CustomPacketPayload {
    public static final Type<InfuseSpiritPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "infuse_spirit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfuseSpiritPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), InfuseSpiritPacket::decode);

    @Override
    public Type<InfuseSpiritPacket> type() { return TYPE; }

    static void encode(InfuseSpiritPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.inventorySlot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.spiritStack);
    }

    static InfuseSpiritPacket decode(RegistryFriendlyByteBuf buffer) {
        return new InfuseSpiritPacket(buffer.readVarInt(), buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    static void handle(InfuseSpiritPacket packet, IPayloadContext context) {
        ServerPlayer sender = context.player() instanceof ServerPlayer player ? player : null;
        if (sender != null) {
            context.enqueueWork(() -> infuseOnServer(sender, packet));
        }
    }

    private static void infuseOnServer(ServerPlayer player, InfuseSpiritPacket packet) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != packet.containerId
                || !menu.stillValid(player)
                || packet.inventorySlot < 0
                || packet.inventorySlot >= player.getInventory().getContainerSize()) {
            return;
        }

        ItemStack blade = player.getInventory().getItem(packet.inventorySlot);
        ItemStack carried = getValidatedSpiritStack(player, menu, packet.spiritStack);
        if (!carried.isEmpty() && IncursusBladeItem.absorbSpiritStack(blade, carried)) {
            player.getInventory().setChanged();
            if (!player.isCreative()) {
                menu.setCarried(carried);
            }
            menu.broadcastChanges();
        }
    }

    private static ItemStack getValidatedSpiritStack(ServerPlayer player,
                                                     AbstractContainerMenu menu,
                                                     ItemStack clientStack) {
        if (!(clientStack.getItem() instanceof SpiritShardItem)
                || clientStack.isEmpty()
                || clientStack.getCount() > clientStack.getMaxStackSize()) {
            return ItemStack.EMPTY;
        }
        if (player.isCreative()) {
            return clientStack.copy();
        }

        ItemStack serverCarried = menu.getCarried();
        if (serverCarried.isEmpty()
                || !ItemStack.isSameItemSameComponents(serverCarried, clientStack)
                || serverCarried.getCount() != clientStack.getCount()) {
            return ItemStack.EMPTY;
        }
        return serverCarried;
    }
}
