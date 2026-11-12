package com.flux_applied.energy;
import net.minecraftforge.energy.IEnergyStorage;
/** Exact long operations for Flux Applied devices; Forge Energy remains available for other mods. */
public interface ILongEnergyStorage extends IEnergyStorage {
    long receiveEnergyLong(long amount, boolean simulate);
    long extractEnergyLong(long amount, boolean simulate);
    long getEnergyStoredLong();
    long getMaxEnergyStoredLong();
    @Override default int receiveEnergy(int amount, boolean simulate) { return (int) receiveEnergyLong(Math.max(0, amount), simulate); }
    @Override default int extractEnergy(int amount, boolean simulate) { return (int) extractEnergyLong(Math.max(0, amount), simulate); }
    @Override default int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, Math.max(0, getEnergyStoredLong())); }
    @Override default int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, Math.max(0, getMaxEnergyStoredLong())); }
}
