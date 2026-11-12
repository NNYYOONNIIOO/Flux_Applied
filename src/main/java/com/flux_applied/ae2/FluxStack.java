package com.flux_applied.ae2;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.flux_applied.item.ItemFluxPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public final class FluxStack implements IAEStack<FluxStack> {
    private long size;
    private long requestable;
    private boolean craftable;
    public FluxStack(long amount) { size = amount; }
    public static long addSaturated(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        if (b < 0 && a < Long.MIN_VALUE - b) return Long.MIN_VALUE;
        return a + b;
    }
    @Override public void add(FluxStack other) {
        if (other != null) { incStackSize(other.size); incCountRequestable(other.requestable); craftable |= other.craftable; }
    }
    @Override public long getStackSize() { return size; }
    @Override public FluxStack setStackSize(long value) { size = value; return this; }
    @Override public long getCountRequestable() { return requestable; }
    @Override public FluxStack setCountRequestable(long value) { requestable = value; return this; }
    @Override public boolean isCraftable() { return craftable; }
    @Override public FluxStack setCraftable(boolean value) { craftable = value; return this; }
    @Override public FluxStack reset() { size = 0; requestable = 0; craftable = false; return this; }
    @Override public boolean isMeaningful() { return size != 0 || requestable != 0 || craftable; }
    @Override public void incStackSize(long value) { size = addSaturated(size, value); }
    @Override public void decStackSize(long value) { size = value == Long.MIN_VALUE ? Long.MAX_VALUE : addSaturated(size, -value); }
    @Override public void incCountRequestable(long value) { requestable = addSaturated(requestable, value); }
    @Override public void decCountRequestable(long value) { requestable = value == Long.MIN_VALUE ? Long.MAX_VALUE : addSaturated(requestable, -value); }
    @Override public void writeToNBT(CompoundNBT tag) { tag.putLong("fe", size); }
    @Override public void writeToPacket(PacketBuffer buffer) { buffer.writeLong(size); }
    @Override public boolean equals(Object other) { return other instanceof FluxStack; }
    @Override public int hashCode() { return 0xFE; }
    @Override public boolean fuzzyComparison(FluxStack other, FuzzyMode mode) { return other != null; }
    @Override public FluxStack copy() { return new FluxStack(size).setCountRequestable(requestable).setCraftable(craftable); }
    @Override public FluxStack empty() { return new FluxStack(0); }
    @Override public IStorageChannel<FluxStack> getChannel() { return FluxStorageChannel.INSTANCE; }
    @Override public ItemStack asItemStackRepresentation() { return ItemFluxPacket.create(1); }
}
