package com.flux_applied.integration.terminal;

import appeng.api.networking.security.IActionSource;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import nyonio.terminal_interaction_integration.api.IContainerHandler;

public class FluxContainerHandler implements IContainerHandler {
    
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
        if (container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        int currentFE = energyStorage.getEnergyStored();
        
        if (currentFE <= 0) {
            return 0;
        }
        
        int toExtract = (int) Math.min(amount, currentFE);
        int extracted = energyStorage.extractEnergy(toExtract, false);
        
        return extracted;
    }
    
    @Override
    public long inject(ItemStack container, long amount, IActionSource source) {
        if (container.isEmpty() || !container.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return 0;
        }
        
        IEnergyStorage energyStorage = container.getCapability(CapabilityEnergy.ENERGY, null);
        int currentFE = energyStorage.getEnergyStored();
        int maxFE = energyStorage.getMaxEnergyStored();
        int space = maxFE - currentFE;
        
        if (space <= 0) {
            return 0;
        }
        
        int toInject = (int) Math.min(amount, space);
        int injected = energyStorage.receiveEnergy(toInject, false);
        
        return injected;
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
