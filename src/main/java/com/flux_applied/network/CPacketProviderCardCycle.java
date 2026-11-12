package com.flux_applied.network;

import appeng.container.slot.RestrictedInputSlot;
import com.flux_applied.FluxApplied;
import com.flux_applied.item.ItemProviderCard;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public final class CPacketProviderCardCycle {
    private final int windowId;
    private final int slotIndex;
    public CPacketProviderCardCycle(int windowId, int slotIndex) {
        this.windowId = windowId;
        this.slotIndex = slotIndex;
    }
    public static void encode(CPacketProviderCardCycle packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.windowId);
        buffer.writeVarInt(packet.slotIndex);
    }
    public static CPacketProviderCardCycle decode(PacketBuffer buffer) {
        return new CPacketProviderCardCycle(buffer.readVarInt(), buffer.readVarInt());
    }
    public static void handle(CPacketProviderCardCycle packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.getSender();
            if (player == null) return;
            Container container = player.openContainer;
            if (container == null || container.windowId != packet.windowId || !container.canInteractWith(player)
                    || packet.slotIndex < 0 || packet.slotIndex >= container.inventorySlots.size()) return;
            Slot slot = container.getSlot(packet.slotIndex);
            if (!(slot instanceof RestrictedInputSlot) || !slot.canTakeStack(player)) return;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || stack.getItem() != FluxApplied.providerCard) return;
            ItemProviderCard.cycleMode(stack);
            slot.onSlotChanged();
            container.detectAndSendChanges();
            ItemProviderCard.sendModeMessage(player, ItemProviderCard.getMode(stack));
        });
        ctx.setPacketHandled(true);
    }
}
