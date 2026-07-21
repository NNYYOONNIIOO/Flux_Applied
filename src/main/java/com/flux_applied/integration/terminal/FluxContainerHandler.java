package com.flux_applied.integration.terminal;

import appeng.api.networking.security.IActionSource;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import nyonio.terminal_interaction_integration.api.IContainerHandler;

public class FluxContainerHandler implements IContainerHandler {

    private static final int MAX_ENERGY_PER_CALL = Integer.MAX_VALUE;
    
    @Override
    public boolean canHandle(ItemStack container) {
        return !container.isEmpty() && container.hasCapability(CapabilityEnergy.ENERGY, null);
    }
    
    @Override
    public long getStoredAmount(ItemStack container) {
        if (container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        return energyStorage.getEnergyStored();
    }
    
    @Override
    public long getMaxCapacity(ItemStack container) {
        if (container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        return energyStorage.getMaxEnergyStored();
    }
    
    @Override
    public long extract(ItemStack container, long amount, IActionSource source) {
        if (amount <= 0 || container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        if (energyStorage == null || !energyStorage.canExtract()) {
            return 0;
        }

        return transferUntilBlocked(amount, energyStorage, false);
    }
    
    @Override
    public long inject(ItemStack container, long amount, IActionSource source) {
        if (amount <= 0 || container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        if (energyStorage == null || !energyStorage.canReceive()) {
            return 0;
        }

        return transferUntilBlocked(amount, energyStorage, true);
    }

    private long transferUntilBlocked(long amount, IEnergyStorage energyStorage, boolean receive) {
        long transferred = 0;

        while (transferred < amount) {
            int request = (int) Math.min(amount - transferred, (long) MAX_ENERGY_PER_CALL);
            int moved = receive
                    ? energyStorage.receiveEnergy(request, false)
                    : energyStorage.extractEnergy(request, false);

            if (moved <= 0) {
                break;
            }

            transferred += Math.min(moved, request);
        }

        return transferred;
    }
    
    @Override
    public String getContainerDisplayName(ItemStack container) {
        if (container.isEmpty()) {
            return "";
        }
        return container.getDisplayName();
    }
    
    public ItemStack getEmptyContainer() {
        return ItemStack.EMPTY;
    }
}
