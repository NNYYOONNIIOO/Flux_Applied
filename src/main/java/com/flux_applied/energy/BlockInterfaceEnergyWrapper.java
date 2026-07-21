package com.flux_applied.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.helpers.MachineSource;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.energy.IEnergyStorage;

public class BlockInterfaceEnergyWrapper implements IEnergyStorage {

    private final IActionHost actionHost;

    public BlockInterfaceEnergyWrapper(IActionHost actionHost) {
        this.actionHost = actionHost;
    }

    @Override
    public boolean canReceive() {
        return getMode() == 0 || getMode() == 2;
    }

    @Override
    public boolean canExtract() {
        return getMode() == 1 || getMode() == 2;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        try {
            IStorageGrid storage = getStorageGrid();
            if (storage == null) return 0;
            MachineSource source = new MachineSource(actionHost);
            FluxStack toInsert = new FluxStack(maxReceive);
            FluxStack remaining = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .injectItems(toInsert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            long inserted = remaining != null ? maxReceive - remaining.getStackSize() : maxReceive;
            return (int) Math.min(inserted, maxReceive);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        try {
            IStorageGrid storage = getStorageGrid();
            if (storage == null) return 0;
            MachineSource source = new MachineSource(actionHost);
            FluxStack request = new FluxStack(maxExtract);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return extracted != null ? (int) Math.min(extracted.getStackSize(), maxExtract) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getEnergyStored() {
        try {
            IStorageGrid storage = getStorageGrid();
            if (storage == null) return 0;
            MachineSource source = new MachineSource(actionHost);
            FluxStack request = new FluxStack(Long.MAX_VALUE);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, Actionable.SIMULATE, source);
            return extracted != null ? (int) Math.min(Integer.MAX_VALUE, extracted.getStackSize()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    private IStorageGrid getStorageGrid() {
        try {
            IGridNode node = actionHost.getActionableNode();
            if (node == null) return null;
            IGrid grid = node.getGrid();
            if (grid == null) return null;
            return grid.getCache(IStorageGrid.class);
        } catch (Exception e) {
            return null;
        }
    }

    private int getMode() {
        if (actionHost instanceof TileEntity) {
            ItemStack card = ProviderCardHelper.findProviderCardInBlockTE((TileEntity) actionHost);
            if (card != null) return ItemProviderCard.getMode(card);
        }
        return 1;
    }
}
