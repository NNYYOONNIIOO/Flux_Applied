package com.flux_applied.ae2;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;

public class FluxCellInventory implements ICellInventory<FluxStack> {

    private static final String FE_TAG = "fe";
    private static final String ITEM_SLOT_0 = "#0";
    private static final String ITEM_COUNT_0 = "@0";

    private final ItemStack itemStack;
    private final ISaveProvider saveProvider;
    private final long totalBytes;
    private long storedFE;
    private boolean isPersisted = true;
    private final NBTTagCompound tagCompound;

    public FluxCellInventory(ItemStack itemStack, ISaveProvider saveProvider, int kilobytes) {
        this.itemStack = itemStack;
        this.saveProvider = saveProvider;

        // 使用二进制单位制：1k = 1024 字节
        this.totalBytes = (long) kilobytes * 1024L;
        
        this.tagCompound = Platform.openNbtData(itemStack);
        
        if (this.tagCompound.hasKey(ITEM_SLOT_0) && this.tagCompound.getCompoundTag(ITEM_SLOT_0).hasKey(FE_TAG)) {
            this.storedFE = this.tagCompound.getCompoundTag(ITEM_SLOT_0).getLong(FE_TAG);
        } else {
            this.storedFE = 0;
        }
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Override
    public double getIdleDrain() {
        return 1.0;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public IItemHandler getConfigInventory() {
        return null;
    }

    @Override
    public IItemHandler getUpgradesInventory() {
        return null;
    }

    @Override
    public int getBytesPerType() {
        return 8;
    }

    @Override
    public boolean canHoldNewItem() {
        return getRemainingItemCount() > 0;
    }

    @Override
    public long getTotalBytes() {
        return this.totalBytes;
    }

    @Override
    public long getFreeBytes() {
        return this.totalBytes - this.getUsedBytes();
    }

    @Override
    public long getUsedBytes() {
        long unitsPerByte = FluxStorageChannel.INSTANCE.getUnitsPerByte();
        return (this.storedFE + unitsPerByte - 1) / unitsPerByte;
    }

    @Override
    public long getTotalItemTypes() {
        return 1;
    }

    @Override
    public long getStoredItemCount() {
        return this.storedFE;
    }

    @Override
    public long getStoredItemTypes() {
        return this.storedFE > 0 ? 1 : 0;
    }

    @Override
    public long getRemainingItemTypes() {
        return this.storedFE > 0 ? 0 : 1;
    }

    @Override
    public long getRemainingItemCount() {
        long maxFE = this.totalBytes * FluxStorageChannel.INSTANCE.getUnitsPerByte();
        return maxFE - this.storedFE;
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public int getStatusForCell() {
        if (this.storedFE == 0) {
            return 4; // 灰色：空
        }
        
        long maxFE = this.totalBytes * FluxStorageChannel.INSTANCE.getUnitsPerByte();
        double fillLevel = (double) this.storedFE / (double) maxFE;
        
        if (fillLevel >= 1.0) {
            return 3; // 红色：已满
        }
        
        if (fillLevel >= 0.75) {
            return 2; // 橙色：接近满（75%以上）
        }
        
        return 1; // 绿色：有空间
    }

    @Override
    public void persist() {
        if (this.isPersisted) {
            return;
        }
        
        if (this.storedFE > 0) {
            NBTTagCompound data = new NBTTagCompound();
            data.setLong(FE_TAG, this.storedFE);
            
            this.tagCompound.setTag(ITEM_SLOT_0, data);
            this.tagCompound.setLong(ITEM_COUNT_0, this.storedFE);
        } else {
            this.tagCompound.removeTag(ITEM_SLOT_0);
            this.tagCompound.removeTag(ITEM_COUNT_0);
        }
        
        this.isPersisted = true;
    }

    @Override
    public FluxStack injectItems(FluxStack input, Actionable mode, IActionSource src) {
        if (input == null) {
            return null;
        }

        long maxFE = this.totalBytes * FluxStorageChannel.INSTANCE.getUnitsPerByte();
        long canInsert = Math.min(maxFE - this.storedFE, input.getStackSize());

        if (mode == Actionable.MODULATE) {
            this.storedFE += canInsert;
            this.saveChanges();
        }

        if (canInsert >= input.getStackSize()) {
            return null;
        }

        FluxStack result = input.copy();
        result.setStackSize(input.getStackSize() - canInsert);
        return result;
    }

    @Override
    public FluxStack extractItems(FluxStack request, Actionable mode, IActionSource src) {
        if (request == null) {
            return null;
        }

        long canExtract = Math.min(this.storedFE, request.getStackSize());

        if (mode == Actionable.MODULATE) {
            this.storedFE -= canExtract;
            this.saveChanges();
        }

        if (canExtract <= 0) {
            return null;
        }

        FluxStack result = new FluxStack(canExtract);
        return result;
    }

    @Override
    public IItemList<FluxStack> getAvailableItems(IItemList<FluxStack> out) {
        if (this.storedFE > 0) {
            out.add(new FluxStack(this.storedFE));
        }
        return out;
    }

    @Override
    public FluxStorageChannel getChannel() {
        return FluxStorageChannel.INSTANCE;
    }

    private void saveChanges() {
        this.isPersisted = false;
        
        // 立即持久化到ItemStack的NBT
        this.persist();
        
        // 通知保存提供者
        if (this.saveProvider != null) {
            this.saveProvider.saveChanges(this);
        }
    }
}