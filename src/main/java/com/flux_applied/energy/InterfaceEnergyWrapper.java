package com.flux_applied.energy;

import appeng.api.parts.IPart;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

public class InterfaceEnergyWrapper implements IEnergyStorage {

    private final IPart part;
    private int cachedMode;

    public InterfaceEnergyWrapper(IPart part, int mode) {
        this.part = part;
        this.cachedMode = mode;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long amount = ProviderCardHelper.injectEnergy(part, maxReceive, simulate);
        return (int) (maxReceive - amount);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        return (int) ProviderCardHelper.extractEnergy(part, maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        long stored = ProviderCardHelper.getNetworkEnergyStored(part);
        return (int) Math.min(stored, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return getMode() == 1 || getMode() == 2;
    }

    @Override
    public boolean canReceive() {
        return getMode() == 0 || getMode() == 2;
    }

    private int getMode() {
        ItemStack card = ProviderCardHelper.findProviderCard(part);
        if (card != null) cachedMode = ItemProviderCard.getMode(card);
        return cachedMode;
    }
}
