package com.flux_applied.integration.terminal;
import appeng.api.networking.security.IActionSource;
import com.flux_applied.energy.ILongEnergyStorage;
import com.flux_applied.util.EnergyTransferHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import nyonio.terminal_interaction_integration.api.IContainerHandler;
public final class FluxContainerHandler implements IContainerHandler {
    private IEnergyStorage energy(ItemStack stack) { return stack.isEmpty() ? null : stack.getCapability(CapabilityEnergy.ENERGY).orElse(null); }
    @Override public boolean canHandle(ItemStack stack) { return energy(stack) != null; }
    @Override public long getStoredAmount(ItemStack stack) {
        IEnergyStorage e = energy(stack);
        return e == null ? 0 : e instanceof ILongEnergyStorage ? ((ILongEnergyStorage)e).getEnergyStoredLong() : Math.max(0, e.getEnergyStored());
    }
    @Override public long getMaxCapacity(ItemStack stack) {
        IEnergyStorage e = energy(stack);
        return e == null ? 0 : e instanceof ILongEnergyStorage ? ((ILongEnergyStorage)e).getMaxEnergyStoredLong() : Math.max(0, e.getMaxEnergyStored());
    }
    @Override public long extract(ItemStack stack, long amount, IActionSource source) { return EnergyTransferHelper.extract(energy(stack), amount); }
    @Override public long inject(ItemStack stack, long amount, IActionSource source) { return EnergyTransferHelper.receive(energy(stack), amount); }
    @Override public String getContainerDisplayName(ItemStack stack) { return stack.getDisplayName().getString(); }
    @Override public ItemStack getEmptyContainer() { return ItemStack.EMPTY; }
}
