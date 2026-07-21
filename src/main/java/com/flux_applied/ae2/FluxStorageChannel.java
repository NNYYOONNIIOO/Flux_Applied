package com.flux_applied.ae2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;

public class FluxStorageChannel implements IStorageChannel<FluxStack> {

    public static final FluxStorageChannel INSTANCE = new FluxStorageChannel();

    @Override
    public int transferFactor() {
        return 1000;
    }

    @Override
    public int getUnitsPerByte() {
        return 10000;  // 每字节存储 10000 FE
    }

    @Nonnull
    @Override
    public IItemList<FluxStack> createList() {
        return new FluxList();
    }

    @Nullable
    @Override
    public FluxStack createStack(@Nonnull Object input) {
        if (input instanceof FluxStack) {
            return ((FluxStack) input).copy();
        }
        return null;
    }

    @Nullable
    @Override
    public FluxStack readFromPacket(@Nonnull ByteBuf input) throws IOException {
        long amount = input.readLong();
        return new FluxStack(amount);
    }

    @Nullable
    @Override
    public FluxStack createFromNBT(@Nonnull NBTTagCompound nbt) {
        long amount = nbt.getLong("fe");
        return new FluxStack(amount);
    }
}