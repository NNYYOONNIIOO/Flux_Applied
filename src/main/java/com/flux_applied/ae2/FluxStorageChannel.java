package com.flux_applied.ae2;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;
import com.flux_applied.item.ItemFluxPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
public final class FluxStorageChannel implements IStorageChannel<FluxStack> {
    public static final FluxStorageChannel INSTANCE = new FluxStorageChannel();
    private FluxStorageChannel() {}
    @Override public int transferFactor() { return 1000; }
    @Override public int getUnitsPerByte() { return 10000; }
    @Override public IItemList<FluxStack> createList() { return new FluxList(); }
    @Override public FluxStack createStack(Object input) {
        if (input instanceof FluxStack) return ((FluxStack) input).copy();
        if (input instanceof ItemStack && ItemFluxPacket.isFluxPacket((ItemStack) input)) return new FluxStack(ItemFluxPacket.getFE((ItemStack) input));
        return null;
    }
    @Override public FluxStack readFromPacket(PacketBuffer buffer) { return new FluxStack(buffer.readLong()); }
    @Override public FluxStack createFromNBT(CompoundNBT tag) { return new FluxStack(tag.getLong("fe")); }
}
