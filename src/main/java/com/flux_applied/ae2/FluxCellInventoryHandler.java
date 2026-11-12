package com.flux_applied.ae2;
import appeng.api.config.AccessRestriction;
import appeng.api.config.IncludeExclude;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.data.IItemList;
public final class FluxCellInventoryHandler implements ICellInventoryHandler<FluxStack> {
    private final FluxCellInventory inventory;
    public FluxCellInventoryHandler(FluxCellInventory inventory) { this.inventory = inventory; }
    @Override public FluxCellInventory getCellInv() { return inventory; }
    @Override public boolean isPreformatted() { return false; }
    @Override public boolean isFuzzy() { return false; }
    @Override public IncludeExclude getIncludeExcludeMode() { return IncludeExclude.WHITELIST; }
    @Override public FluxStack injectItems(FluxStack input, appeng.api.config.Actionable action, appeng.api.networking.security.IActionSource source) { return inventory.injectItems(input, action, source); }
    @Override public FluxStack extractItems(FluxStack request, appeng.api.config.Actionable action, appeng.api.networking.security.IActionSource source) { return inventory.extractItems(request, action, source); }
    @Override public IItemList<FluxStack> getAvailableItems(IItemList<FluxStack> out) { return inventory.getAvailableItems(out); }
    @Override public FluxStorageChannel getChannel() { return FluxStorageChannel.INSTANCE; }
    @Override public AccessRestriction getAccess() { return AccessRestriction.READ_WRITE; }
    @Override public boolean isPrioritized(FluxStack input) { return false; }
    @Override public boolean canAccept(FluxStack input) { return input != null && inventory.canHoldNewItem(); }
    @Override public int getPriority() { return 0; }
    @Override public int getSlot() { return 0; }
    @Override public boolean validForPass(int pass) { return true; }
}
