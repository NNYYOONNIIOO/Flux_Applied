package com.flux_applied.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.flux_applied.ae2.IFluxStorageCell;

import javax.annotation.Nullable;
import java.util.List;

public class ItemFluxStorageCell extends Item implements IEnergyStorage, IFluxStorageCell
{
    private final int kilobytes;
    private final long maxEnergy;

    public ItemFluxStorageCell(int kilobytes) {
        this.kilobytes = kilobytes;
        // 计算实际能量容量：kilobytes × 1024 bytes × 10000 FE/byte
        this.maxEnergy = (long) kilobytes * 1024L * 10000L;
        this.setMaxStackSize(1);
    }

    @Override
    public long getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public int getKilobytes() {
        return kilobytes;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new FluxEnergyStorage(stack, maxEnergy);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        long stored = getStoredEnergyFromNBT(stack);
        long totalBytes = (long) kilobytes * 1024L;
        long usedBytes = (stored + 9999L) / 10000L;

        if (totalBytes > 0) {
            tooltip.add(usedBytes + "/" + totalBytes + " \u5b57\u8282\u5df2\u4f7f\u7528");
            tooltip.add("\u5b58\u50a8\u4e86 " + stored + " FE");
        }
    }

    private long getStoredEnergyFromNBT(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return 0;
        }

        // AE2 存储单元格式: "#0": {fe: XXXX}
        if (tag.hasKey("#0")) {
            NBTTagCompound dataTag = tag.getCompoundTag("#0");
            if (dataTag.hasKey("fe")) {
                return dataTag.getLong("fe");
            }
        }

        // 旧格式/手持格式: "Energy": XXXX
        if (tag.hasKey("Energy")) {
            return tag.getLong("Energy");
        }

        return 0;
    }

    private static class FluxEnergyStorage implements IEnergyStorage, ICapabilityProvider
    {
        private final ItemStack stack;
        private final long capacity;

        public FluxEnergyStorage(ItemStack stack, long capacity) {
            this.stack = stack;
            this.capacity = capacity;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == CapabilityEnergy.ENERGY) {
                return CapabilityEnergy.ENERGY.cast(this);
            }
            return null;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }

            long stored = getStoredLong();
            long toReceive = Math.min(capacity - stored, Math.min((long) maxReceive, Integer.MAX_VALUE));
            int received = (int) Math.min(toReceive, Integer.MAX_VALUE);

            if (!simulate) {
                setStoredLong(stored + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract()) {
                return 0;
            }

            long stored = getStoredLong();
            long toExtract = Math.min(stored, Math.min((long) maxExtract, Integer.MAX_VALUE));
            int extracted = (int) Math.min(toExtract, Integer.MAX_VALUE);

            if (!simulate) {
                setStoredLong(stored - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(getStoredLong(), Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(capacity, Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private int getMaxTransfer() {
            return Integer.MAX_VALUE;
        }

        private long getStoredLong() {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                return 0;
            }

            // AE2 存储单元格式: "#0": {fe: XXXX}
            if (tag.hasKey("#0")) {
                NBTTagCompound dataTag = tag.getCompoundTag("#0");
                if (dataTag.hasKey("fe")) {
                    return dataTag.getLong("fe");
                }
            }

            // 旧格式/手持格式: "Energy": XXXX
            if (tag.hasKey("Energy")) {
                return tag.getLong("Energy");
            }

            return 0;
        }

        private void setStoredLong(long energy) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }

            // 如果存在 AE2 格式的标签，更新它
            if (tag.hasKey("#0")) {
                NBTTagCompound dataTag = tag.getCompoundTag("#0");
                dataTag.setLong("fe", energy);
                tag.setTag("#0", dataTag);
                
                // 同步更新 @0 计数
                tag.setLong("@0", energy);
            } else {
                // 否则使用旧格式
                tag.setLong("Energy", energy);
            }
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}