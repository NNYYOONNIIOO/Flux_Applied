package com.flux_applied.ae2;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.data.IItemList;
import com.flux_applied.item.ItemFluxStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import javax.annotation.Nullable;
public final class FluxCellInventory implements ICellInventory<FluxStack> {
    private final ItemStack itemStack;
    @Nullable private final ISaveProvider saveProvider;
    private final long totalBytes;
    private long storedFE;
    public FluxCellInventory(ItemStack itemStack, @Nullable ISaveProvider saveProvider, int kilobytes) {
        this.itemStack = itemStack;
        this.saveProvider = saveProvider;
        this.totalBytes = (long) kilobytes * 1024L;
        this.storedFE = Math.min(capacity(), ItemFluxStorageCell.readEnergy(itemStack));
    }
    @Override public ItemStack getItemStack() { return itemStack; }
    @Override public double getIdleDrain() { return 1.0; }
    @Override public FuzzyMode getFuzzyMode() { return FuzzyMode.IGNORE_ALL; }
    @Nullable @Override public IItemHandler getConfigInventory() { return null; }
    @Nullable @Override public IItemHandler getUpgradesInventory() { return null; }
    @Override public int getBytesPerType() { return 8; }
    @Override public boolean canHoldNewItem() { return storedFE < capacity(); }
    @Override public long getTotalBytes() { return totalBytes; }
    @Override public long getFreeBytes() { return totalBytes - getUsedBytes(); }
    @Override public long getUsedBytes() { return (storedFE + 9999L) / 10000L; }
    @Override public long getTotalItemTypes() { return 1; }
    @Override public long getStoredItemCount() { return storedFE; }
    @Override public long getStoredItemTypes() { return storedFE > 0 ? 1 : 0; }
    @Override public long getRemainingItemTypes() { return storedFE > 0 ? 0 : 1; }
    @Override public long getRemainingItemCount() { return Math.max(0, capacity() - storedFE); }
    @Override public int getUnusedItemCount() { return 0; }
    @Override public CellState getStatusForCell() { return storedFE <= 0 ? CellState.EMPTY : storedFE >= capacity() ? CellState.FULL : CellState.NOT_EMPTY; }
    @Override public void persist() {
        ItemFluxStorageCell.writeEnergy(itemStack, storedFE);
    }
    @Override public FluxStack injectItems(FluxStack input, Actionable action, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) return input;
        long accepted = Math.min(Math.max(0, capacity() - storedFE), input.getStackSize());
        if (accepted > 0 && action == Actionable.MODULATE) { storedFE += accepted; changed(); }
        long remainder = input.getStackSize() - accepted;
        return remainder == 0 ? null : new FluxStack(remainder);
    }
    @Override public FluxStack extractItems(FluxStack request, Actionable action, IActionSource source) {
        if (request == null || request.getStackSize() <= 0 || storedFE <= 0) return null;
        long extracted = Math.min(storedFE, request.getStackSize());
        if (action == Actionable.MODULATE) { storedFE -= extracted; changed(); }
        return new FluxStack(extracted);
    }
    @Override public IItemList<FluxStack> getAvailableItems(IItemList<FluxStack> out) {
        if (storedFE > 0) out.addStorage(new FluxStack(storedFE));
        return out;
    }
    @Override public FluxStorageChannel getChannel() { return FluxStorageChannel.INSTANCE; }
    private long capacity() { return totalBytes * FluxStorageChannel.INSTANCE.getUnitsPerByte(); }
    private void changed() { persist(); if (saveProvider != null) saveProvider.saveChanges(this); }
}
