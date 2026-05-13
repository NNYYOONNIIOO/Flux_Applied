package com.flux_applied.ae2;

import javax.annotation.Nullable;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.data.IItemList;

public class FluxCellInventoryHandler implements ICellInventoryHandler<FluxStack> {

    private final FluxCellInventory inventory;

    public FluxCellInventoryHandler(FluxCellInventory inventory) {
        this.inventory = inventory;
    }

    @Nullable
    @Override
    public FluxCellInventory getCellInv() {
        return this.inventory;
    }

    @Override
    public boolean isPreformatted() {
        return false;
    }

    @Override
    public boolean isFuzzy() {
        return false;
    }

    @Override
    public IncludeExclude getIncludeExcludeMode() {
        return IncludeExclude.WHITELIST;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(FluxStack input) {
        return false;
    }

    @Override
    public boolean canAccept(FluxStack input) {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int i) {
        return true;
    }

    @Override
    public FluxStack injectItems(FluxStack input, appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource src) {
        return this.inventory.injectItems(input, mode, src);
    }

    @Override
    public FluxStack extractItems(FluxStack request, appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource src) {
        return this.inventory.extractItems(request, mode, src);
    }

    @Override
    public IItemList<FluxStack> getAvailableItems(IItemList<FluxStack> out) {
        return this.inventory.getAvailableItems(out);
    }

    @Override
    public FluxStorageChannel getChannel() {
        return FluxStorageChannel.INSTANCE;
    }

    public void persist() {
        this.inventory.persist();
    }
}