package com.flux_applied.ae2;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;

import com.flux_applied.item.ItemFluxPacket;

public class FluxStack implements IAEStack<FluxStack> {

    public static final FluxStack STATIC_STACK = new FluxStack(0);

    private long stackSize;
    private long countRequestable;
    private boolean isCraftable;

    public FluxStack(long amount) {
        this.stackSize = amount;
        this.countRequestable = 0;
        this.isCraftable = false;
    }

    @Override
    public void add(FluxStack is) {
        if (is != null) {
            this.stackSize += is.stackSize;
        }
    }

    @Override
    public long getStackSize() {
        return this.stackSize;
    }

    @Override
    public FluxStack setStackSize(long stackSize) {
        this.stackSize = stackSize;
        return this;
    }

    @Override
    public long getCountRequestable() {
        return this.countRequestable;
    }

    @Override
    public FluxStack setCountRequestable(long countRequestable) {
        this.countRequestable = countRequestable;
        return this;
    }

    @Override
    public boolean isCraftable() {
        return this.isCraftable;
    }

    @Override
    public FluxStack setCraftable(boolean isCraftable) {
        this.isCraftable = isCraftable;
        return this;
    }

    @Override
    public FluxStack reset() {
        this.stackSize = 0;
        this.countRequestable = 0;
        this.isCraftable = false;
        return this;
    }

    @Override
    public boolean isMeaningful() {
        return this.stackSize != 0 || this.countRequestable != 0 || this.isCraftable;
    }

    @Override
    public void incStackSize(long i) {
        this.stackSize += i;
    }

    @Override
    public void decStackSize(long i) {
        this.stackSize -= i;
    }

    @Override
    public void incCountRequestable(long i) {
        this.countRequestable += i;
    }

    @Override
    public void decCountRequestable(long i) {
        this.countRequestable -= i;
    }

    @Override
    public void writeToNBT(NBTTagCompound i) {
        i.setLong("fe", this.stackSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FluxStack) {
            return true;
        }
        return false;
    }

    @Override
    public boolean fuzzyComparison(FluxStack other, FuzzyMode mode) {
        return true;
    }

    @Override
    public void writeToPacket(ByteBuf data) throws IOException {
        data.writeLong(this.stackSize);
    }

    @Override
    public FluxStack copy() {
        FluxStack stack = new FluxStack(this.stackSize);
        stack.setCountRequestable(this.countRequestable);
        stack.setCraftable(this.isCraftable);
        return stack;
    }

    @Override
    public FluxStack empty() {
        return new FluxStack(0);
    }

    @Override
    public boolean isItem() {
        return false;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public IStorageChannel<FluxStack> getChannel() {
        return FluxStorageChannel.INSTANCE;
    }

    @Override
    public ItemStack asItemStackRepresentation() {
        return ItemFluxPacket.create(this.stackSize);
    }
}