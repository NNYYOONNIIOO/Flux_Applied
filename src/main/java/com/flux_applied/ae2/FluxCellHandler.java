package com.flux_applied.ae2;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.data.IAEStack;
import com.flux_applied.item.ItemFluxStorageCell;
import net.minecraft.item.ItemStack;
import javax.annotation.Nullable;
public final class FluxCellHandler implements ICellHandler {
    public static final FluxCellHandler INSTANCE = new FluxCellHandler();
    private FluxCellHandler() {}
    @Override public boolean isCell(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ItemFluxStorageCell; }
    @Nullable @Override public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(ItemStack stack, @Nullable ISaveProvider host, IStorageChannel<T> channel) {
        if (!isCell(stack) || channel != FluxStorageChannel.INSTANCE) return null;
        FluxCellInventory inv = new FluxCellInventory(stack, host, ((ItemFluxStorageCell) stack.getItem()).getKilobytes());
        @SuppressWarnings("unchecked") ICellInventoryHandler<T> result = (ICellInventoryHandler<T>) new FluxCellInventoryHandler(inv);
        return result;
    }
}
