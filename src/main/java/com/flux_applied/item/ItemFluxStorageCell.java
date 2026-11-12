package com.flux_applied.item;

import com.flux_applied.FluxApplied;
import com.flux_applied.ae2.IFluxStorageCell;
import com.flux_applied.energy.ILongEnergyStorage;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import java.util.List;
import javax.annotation.Nullable;

public final class ItemFluxStorageCell extends Item implements IFluxStorageCell {
    private final int kilobytes;
    private final long capacity;
    public ItemFluxStorageCell(int kilobytes) {
        super(FluxApplied.properties().maxStackSize(1));
        this.kilobytes = kilobytes;
        capacity = (long) kilobytes * 1024L * 10000L;
    }
    @Override public int getKilobytes() { return kilobytes; }
    @Override public long getMaxEnergy() { return capacity; }
    public static long readEnergy(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null) return 0;
        return Math.max(0, tag.contains("#0") ? tag.getCompound("#0").getLong("fe") : tag.getLong("Energy"));
    }
    public static void writeEnergy(ItemStack stack, long energy) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.remove("Energy");
        if (energy > 0) {
            CompoundNBT resource = new CompoundNBT();
            resource.putLong("fe", energy);
            tag.put("#0", resource);
            tag.putLong("@0", energy);
        } else {
            tag.remove("#0"); tag.remove("@0");
        }
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) { return new CellEnergy(stack, capacity); }
    @Override public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        long stored = Math.min(capacity, readEnergy(stack));
        tooltip.add(new TranslationTextComponent("flux_applied.tooltip.bytes_used", (stored + 9999) / 10000, (long) kilobytes * 1024));
        tooltip.add(new TranslationTextComponent("flux_applied.tooltip.stored_fe", stored));
    }
    public static final class CellEnergy implements ILongEnergyStorage, ICapabilityProvider {
        private final ItemStack stack;
        private final long capacity;
        private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> energy = LazyOptional.of(() -> this);
        public CellEnergy(ItemStack stack, long capacity) { this.stack = stack; this.capacity = capacity; }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) { return capability == CapabilityEnergy.ENERGY ? energy.cast() : LazyOptional.empty(); }
        @Override public long getEnergyStoredLong() { return Math.min(capacity, readEnergy(stack)); }
        @Override public long getMaxEnergyStoredLong() { return capacity; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
        @Override public long receiveEnergyLong(long amount, boolean simulate) {
            long stored = getEnergyStoredLong();
            long moved = Math.min(Math.max(0, amount), capacity - stored);
            if (!simulate && moved > 0) writeEnergy(stack, stored + moved);
            return moved;
        }
        @Override public long extractEnergyLong(long amount, boolean simulate) {
            long stored = getEnergyStoredLong();
            long moved = Math.min(Math.max(0, amount), stored);
            if (!simulate && moved > 0) writeEnergy(stack, stored - moved);
            return moved;
        }
    }
}
