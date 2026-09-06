package org.brahypno.maledict.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import org.brahypno.maledict.common.item.IncursusBladeItem;

import java.util.function.Supplier;

public record InfuseSpiritPacket(int containerId, int inventorySlot, ItemStack spiritStack) {
    static void encode(InfuseSpiritPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.inventorySlot);
        buffer.writeItem(packet.spiritStack);
    }

    static InfuseSpiritPacket decode(FriendlyByteBuf buffer) {
        return new InfuseSpiritPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readItem());
    }

    static void handle(InfuseSpiritPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> infuseOnServer(sender, packet));
        }
        context.setPacketHandled(true);
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
                || !ItemStack.isSameItemSameTags(serverCarried, clientStack)
                || serverCarried.getCount() != clientStack.getCount()) {
            return ItemStack.EMPTY;
        }
        return serverCarried;
    }
}
