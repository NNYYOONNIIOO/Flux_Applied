package com.flux_applied.util;

import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyTransferHelper {
    private static final long MAX_INT = Integer.MAX_VALUE;

    private EnergyTransferHelper() {
    }

    public static long receive(IEnergyStorage storage, long amount) {
        return transfer(storage, amount, true);
    }

    public static long extract(IEnergyStorage storage, long amount) {
        return transfer(storage, amount, false);
    }

    private static long transfer(IEnergyStorage storage, long amount, boolean receive) {
        if (storage == null || amount <= 0) {
            return 0;
        }

        long movedTotal = 0;
        while (movedTotal < amount) {
            int request = (int) Math.min(amount - movedTotal, MAX_INT);
            int moved = receive
                    ? storage.receiveEnergy(request, false)
                    : storage.extractEnergy(request, false);
            if (moved <= 0) {
                break;
            }
            movedTotal += Math.min(moved, request);
        }
        return movedTotal;
    }
}
