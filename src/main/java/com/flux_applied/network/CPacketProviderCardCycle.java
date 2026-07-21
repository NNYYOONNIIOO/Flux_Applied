package com.flux_applied.network;

import com.flux_applied.FluxApplied;
import com.flux_applied.item.ItemProviderCard;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CPacketProviderCardCycle implements IMessage {

    private int slotIndex;

    public CPacketProviderCardCycle() {
    }

    public CPacketProviderCardCycle(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotIndex);
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public static class Handler implements IMessageHandler<CPacketProviderCardCycle, IMessage> {

        @Override
        public IMessage onMessage(CPacketProviderCardCycle message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer == null) return;

                if (message.getSlotIndex() < 0 || message.getSlotIndex() >= player.openContainer.inventorySlots.size()) return;

                Slot slot = player.openContainer.getSlot(message.getSlotIndex());
                if (slot == null) return;

                ItemStack stackInSlot = slot.getStack();
                if (stackInSlot.isEmpty() || stackInSlot.getItem() != FluxApplied.providerCard) return;

                ItemProviderCard.cycleMode(stackInSlot);
            });
            return null;
        }
    }
}
