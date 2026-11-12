package com.flux_applied.util;

import com.flux_applied.energy.ILongEnergyStorage;
import com.flux_applied.energy.NetworkEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyTransferHelper {
    private static final int MAX_FORGE_CALLS = 4096;

    private EnergyTransferHelper() {}

    public static long receive(IEnergyStorage storage, long amount) {
        return transfer(storage, amount, true, false);
    }

    public static long extract(IEnergyStorage storage, long amount) {
        return transfer(storage, amount, false, false);
    }

    public static long receive(IEnergyStorage storage, long amount, boolean simulate) {
        return transfer(storage, amount, true, simulate);
    }

    public static long extract(IEnergyStorage storage, long amount, boolean simulate) {
        return transfer(storage, amount, false, simulate);
    }

    public static long push(IEnergyStorage source, IEnergyStorage target, long amount) {
        if (source == null || target == null || source == target || amount <= 0
                || !source.canExtract() || !target.canReceive() || sameNetwork(source, target)) {
            return 0;
        }

        if (source instanceof ILongEnergyStorage && target instanceof ILongEnergyStorage) {
            long available = extract(source, amount, true);
            long room = receive(target, available, true);
            long requested = Math.min(available, room);
            if (requested <= 0) return 0;

            long extracted = extract(source, requested, false);
            long accepted = receive(target, extracted, false);
            refund(source, extracted - accepted);
            return accepted;
        }

        long moved = 0;
        for (int calls = 0; calls < MAX_FORGE_CALLS && moved < amount; calls++) {
            long request = Math.min(amount - moved, Integer.MAX_VALUE);
            long available = extract(source, request, true);
            long receivable = receive(target, available, true);
            long planned = Math.min(available, receivable);
            if (planned <= 0) break;

            long extracted = extract(source, planned, false);
            if (extracted <= 0) break;
            long accepted = receive(target, extracted, false);
            refund(source, extracted - accepted);
            moved += accepted;
            if (accepted < planned || extracted < planned) break;
        }
        return moved;
    }

    private static long transfer(IEnergyStorage storage, long amount, boolean receive, boolean simulate) {
        if (storage == null || amount <= 0 || (receive ? !storage.canReceive() : !storage.canExtract())) {
            return 0;
        }
        if (storage instanceof ILongEnergyStorage) {
            ILongEnergyStorage exact = (ILongEnergyStorage) storage;
            long moved = receive ? exact.receiveEnergyLong(amount, simulate) : exact.extractEnergyLong(amount, simulate);
            return Math.max(0, Math.min(amount, moved));
        }

        if (simulate) {
            int request = (int) Math.min(amount, Integer.MAX_VALUE);
            int moved = receive ? storage.receiveEnergy(request, true) : storage.extractEnergy(request, true);
            return Math.max(0, Math.min(request, moved));
        }

        long total = 0;
        for (int calls = 0; calls < MAX_FORGE_CALLS && total < amount; calls++) {
            int request = (int) Math.min(amount - total, Integer.MAX_VALUE);
            int moved = receive ? storage.receiveEnergy(request, false) : storage.extractEnergy(request, false);
            if (moved <= 0) break;
            total += Math.min(moved, request);
        }
        return total;
    }

    private static void refund(IEnergyStorage source, long amount) {
        if (amount <= 0) return;
        if (source instanceof NetworkEnergyStorage) {
            ((NetworkEnergyStorage) source).refund(amount);
        } else if (source.canReceive()) {
            receive(source, amount);
        }
    }

    public static boolean sameNetwork(IEnergyStorage first, IEnergyStorage second) {
        if (first == second) return true;
        return first instanceof NetworkEnergyStorage && second instanceof NetworkEnergyStorage
                && ((NetworkEnergyStorage) first).inventory() != null
                && ((NetworkEnergyStorage) first).inventory() == ((NetworkEnergyStorage) second).inventory();
    }
}
