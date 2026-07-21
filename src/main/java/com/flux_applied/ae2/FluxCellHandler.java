package com.flux_applied.ae2;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;

public class FluxCellHandler implements ICellHandler {

    public static final FluxCellHandler INSTANCE = new FluxCellHandler();

    @Override
    public boolean isCell(ItemStack is) {
        if (is == null || is.isEmpty()) {
            return false;
        }
        return is.getItem() instanceof IFluxStorageCell;
    }

    @Override
    @Nullable
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(ItemStack is, ISaveProvider host, IStorageChannel<T> channel) {
        if (!this.isCell(is)) {
            return null;
        }

        if (channel != FluxStorageChannel.INSTANCE) {
            return null;
        }

        IFluxStorageCell cellItem = (IFluxStorageCell) is.getItem();
        FluxCellInventory inventory = new FluxCellInventory(is, host, cellItem.getKilobytes());
        @SuppressWarnings("unchecked")
        ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) new FluxCellInventoryHandler(inventory);
        return handler;
    }
}