package com.flux_applied.util;

import com.flux_applied.energy.ILongEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnergyTransferHelperTest {
    @Test public void movesLongMaximumInOneNativeOperation() {
        ExactStorage source = new ExactStorage(Long.MAX_VALUE, Long.MAX_VALUE);
        ExactStorage target = new ExactStorage(0, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, EnergyTransferHelper.push(source, target, Long.MAX_VALUE));
        assertEquals(0, source.stored);
        assertEquals(Long.MAX_VALUE, target.stored);
        assertEquals(1, source.extractions);
        assertEquals(1, target.insertions);
    }

    @Test public void forgeStorageCanMoveMoreThanOneInt() {
        long amount = (long) Integer.MAX_VALUE * 3 + 17;
        IntStorage source = new IntStorage(amount, amount);
        IntStorage target = new IntStorage(0, amount);
        assertEquals(amount, EnergyTransferHelper.push(source, target, amount));
        assertEquals(0, source.stored);
        assertEquals(amount, target.stored);
        assertEquals(4, source.extractions);
    }

    @Test public void aFullTargetNeverDrainsTheSource() {
        ExactStorage source = new ExactStorage(400, 400);
        ExactStorage target = new ExactStorage(300, 300);
        assertEquals(0, EnergyTransferHelper.push(source, target, 400));
        assertEquals(400, source.stored);
        assertEquals(0, source.extractions);
    }

    @Test public void simulationDoesNotRepeatedlyCountTheSameEnergy() {
        IntStorage source = new IntStorage(100, 100);
        assertEquals(100, EnergyTransferHelper.extract(source, Long.MAX_VALUE, true));
        assertEquals(100, source.stored);
        assertEquals(0, source.extractions);
    }

    @Test public void infiniteForgeDevicesCannotCreateAnUnboundedTickLoop() {
        IEnergyStorage infinite = new IEnergyStorage() {
            public int receiveEnergy(int amount, boolean simulate) { return amount; }
            public int extractEnergy(int amount, boolean simulate) { return amount; }
            public int getEnergyStored() { return Integer.MAX_VALUE; }
            public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
            public boolean canReceive() { return true; }
            public boolean canExtract() { return true; }
        };
        long moved = EnergyTransferHelper.receive(infinite, Long.MAX_VALUE);
        assertEquals((long) Integer.MAX_VALUE * 4096, moved);
    }

    @Test public void rejectedRealTransferIsRefunded() {
        ExactStorage source = new ExactStorage(1000, 1000);
        ExactStorage target = new ExactStorage(0, 1000) {
            @Override public long receiveEnergyLong(long amount, boolean simulate) {
                return super.receiveEnergyLong(simulate ? amount : Math.min(70, amount), simulate);
            }
        };
        assertEquals(70, EnergyTransferHelper.push(source, target, 1000));
        assertEquals(930, source.stored);
        assertEquals(1000, source.stored + target.stored);
    }

    @Test public void negativeRequestsAndSelfTransfersDoNothing() {
        ExactStorage source = new ExactStorage(100, 100);
        assertEquals(0, EnergyTransferHelper.push(source, source, 100));
        assertEquals(0, EnergyTransferHelper.extract(source, -1));
        assertEquals(0, EnergyTransferHelper.receive(source, -1));
        assertEquals(100, source.stored);
    }

    private static class ExactStorage implements ILongEnergyStorage {
        long stored;
        final long capacity;
        int extractions, insertions;
        ExactStorage(long stored, long capacity) { this.stored = stored; this.capacity = capacity; }
        public boolean canReceive() { return true; }
        public boolean canExtract() { return true; }
        public long getEnergyStoredLong() { return stored; }
        public long getMaxEnergyStoredLong() { return capacity; }
        public long receiveEnergyLong(long amount, boolean simulate) {
            long moved = Math.min(amount, capacity - stored);
            if (!simulate) { stored += moved; insertions++; }
            return moved;
        }
        public long extractEnergyLong(long amount, boolean simulate) {
            long moved = Math.min(amount, stored);
            if (!simulate) { stored -= moved; extractions++; }
            return moved;
        }
    }

    private static final class IntStorage implements IEnergyStorage {
        long stored;
        final long capacity;
        int extractions;
        IntStorage(long stored, long capacity) { this.stored = stored; this.capacity = capacity; }
        public boolean canReceive() { return true; }
        public boolean canExtract() { return true; }
        public int getEnergyStored() { return (int) Math.min(stored, Integer.MAX_VALUE); }
        public int getMaxEnergyStored() { return (int) Math.min(capacity, Integer.MAX_VALUE); }
        public int receiveEnergy(int amount, boolean simulate) {
            int moved = (int) Math.min(amount, capacity - stored);
            if (!simulate) stored += moved;
            return moved;
        }
        public int extractEnergy(int amount, boolean simulate) {
            int moved = (int) Math.min(amount, stored);
            if (!simulate) { stored -= moved; extractions++; }
            return moved;
        }
    }
}
