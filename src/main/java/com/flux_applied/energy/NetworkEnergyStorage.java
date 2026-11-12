package com.flux_applied.energy;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.helpers.MachineSource;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
/** One adapter per host, with a single shared transfer budget per direction and server tick. */
public class NetworkEnergyStorage implements ILongEnergyStorage {
    private final Supplier<IActionHost> host;
    private final IntSupplier mode;
    private final BooleanSupplier present;
    private final LongSupplier rate, tick;
    private long lastTick = Long.MIN_VALUE;
    private long input, output;
    public NetworkEnergyStorage(Supplier<IActionHost> host, IntSupplier mode, BooleanSupplier present, LongSupplier rate, LongSupplier tick) {
        this.host = host; this.mode = mode; this.present = present; this.rate = rate; this.tick = tick;
    }
    private void updateTick() { long now = tick.getAsLong(); if (lastTick != now) { lastTick = now; input = output = 0; } }
    public IMEMonitor<FluxStack> inventory() {
        IActionHost h = host.get();
        if (h == null || !present.getAsBoolean()) return null;
        IGridNode node = h.getActionableNode();
        if (node == null || !node.isActive() || node.getGrid() == null) return null;
        IStorageGrid storage = (IStorageGrid) node.getGrid().getCache(IStorageGrid.class);
        return storage.getInventory(FluxStorageChannel.INSTANCE);
    }
    @Override public boolean canExtract() { return mode.getAsInt() != 0 && inventory() != null; }
    @Override public boolean canReceive() { return mode.getAsInt() != 1 && inventory() != null; }
    @Override public long getEnergyStoredLong() {
        IMEMonitor<FluxStack> inv = inventory();
        if (inv == null) return 0;
        FluxStack result = inv.extractItems(new FluxStack(Long.MAX_VALUE), Actionable.SIMULATE, new MachineSource(host.get()));
        return result == null ? 0 : Math.max(0, result.getStackSize());
    }
    @Override public long getMaxEnergyStoredLong() { return Long.MAX_VALUE; }
    @Override public long receiveEnergyLong(long amount, boolean simulate) {
        updateTick();
        IMEMonitor<FluxStack> inv = inventory();
        if (amount <= 0 || mode.getAsInt() == 1 || inv == null) return 0;
        long request = Math.min(amount, Math.max(0, rate.getAsLong() - input));
        if (request <= 0) return 0;
        FluxStack remaining = inv.injectItems(new FluxStack(request), simulate ? Actionable.SIMULATE : Actionable.MODULATE, new MachineSource(host.get()));
        long accepted = request - (remaining == null ? 0 : Math.max(0, Math.min(request, remaining.getStackSize())));
        if (!simulate) input += accepted;
        return accepted;
    }
    @Override public long extractEnergyLong(long amount, boolean simulate) {
        updateTick();
        IMEMonitor<FluxStack> inv = inventory();
        if (amount <= 0 || mode.getAsInt() == 0 || inv == null) return 0;
        long request = Math.min(amount, Math.max(0, rate.getAsLong() - output));
        if (request <= 0) return 0;
        FluxStack result = inv.extractItems(new FluxStack(request), simulate ? Actionable.SIMULATE : Actionable.MODULATE, new MachineSource(host.get()));
        long extracted = result == null ? 0 : Math.max(0, Math.min(request, result.getStackSize()));
        if (!simulate) output += extracted;
        return extracted;
    }
    /** Return energy a receiving device rejected after a successful simulation. */
    public void refund(long amount) {
        if (amount <= 0) return;
        IMEMonitor<FluxStack> inv = inventory();
        if (inv != null) inv.injectItems(new FluxStack(amount), Actionable.MODULATE, new MachineSource(host.get()));
        output = Math.max(0, output - amount);
    }
}
