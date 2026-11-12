package com.flux_applied.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import com.flux_applied.ae2.FluxStack;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NetworkEnergyStorageTest {
    @Test public void allLookupsShareThePerTickInputBudget() {
        Fixture f = new Fixture(100);
        assertEquals(80, f.storage.receiveEnergyLong(80, false));
        assertEquals(20, f.storage.receiveEnergyLong(80, false));
        assertEquals(0, f.storage.receiveEnergyLong(80, false));
        f.tick.incrementAndGet();
        assertEquals(100, f.storage.receiveEnergyLong(100, false));
        assertEquals(200, f.amount.get());
    }
    @Test public void simulationDoesNotConsumeTheBudget() {
        Fixture f = new Fixture(100);
        assertEquals(100, f.storage.receiveEnergyLong(1000, true));
        assertEquals(100, f.storage.receiveEnergyLong(1000, true));
        assertEquals(0, f.amount.get());
        assertEquals(100, f.storage.receiveEnergyLong(1000, false));
    }
    @Test public void outputBudgetIsSharedAndIndependentFromInput() {
        Fixture f = new Fixture(100);
        f.amount.set(1000);
        assertEquals(90, f.storage.extractEnergyLong(90, false));
        assertEquals(10, f.storage.extractEnergyLong(90, false));
        assertEquals(0, f.storage.extractEnergyLong(90, false));
        assertEquals(100, f.storage.receiveEnergyLong(100, false));
        assertEquals(1000, f.amount.get());
    }
    @Test public void longMaximumDoesNotOverflowBudgetAccounting() {
        Fixture f = new Fixture(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, f.storage.receiveEnergyLong(Long.MAX_VALUE, false));
        assertEquals(0, f.storage.receiveEnergyLong(1, false));
        assertEquals(Long.MAX_VALUE, f.storage.extractEnergyLong(Long.MAX_VALUE, false));
        assertEquals(0, f.storage.extractEnergyLong(1, false));
        assertEquals(0, f.amount.get());
    }
    @Test public void refundsWorkInOutputOnlyModeWithoutUsingInputBudget() {
        Fixture f = new Fixture(100);
        f.mode = 1;
        f.amount.set(100);
        assertEquals(100, f.storage.extractEnergyLong(100, false));
        f.storage.refund(30);
        assertEquals(30, f.amount.get());
        assertEquals(30, f.storage.extractEnergyLong(100, false));
    }
    @Test public void disabledDirectionsAndNegativeAmountsDoNothing() {
        Fixture f = new Fixture(100);
        f.mode = 0;
        f.amount.set(100);
        assertEquals(0, f.storage.extractEnergyLong(100, false));
        f.mode = 1;
        assertEquals(0, f.storage.receiveEnergyLong(100, false));
        assertEquals(0, f.storage.extractEnergyLong(-1, false));
        assertEquals(100, f.amount.get());
    }
    private static final class Fixture {
        final AtomicLong amount = new AtomicLong();
        final AtomicLong tick = new AtomicLong();
        final IMEMonitor<FluxStack> inventory;
        final NetworkEnergyStorage storage;
        int mode = 2;
        @SuppressWarnings("unchecked")
        Fixture(long rate) {
            inventory = mock(IMEMonitor.class);
            IActionHost host = mock(IActionHost.class);
            storage = new NetworkEnergyStorage(() -> host, () -> mode, () -> true, () -> rate, tick::get) {
                @Override public IMEMonitor<FluxStack> inventory() { return inventory; }
            };
            when(inventory.injectItems(any(FluxStack.class), any(Actionable.class), any(IActionSource.class))).thenAnswer(call -> {
                FluxStack request = call.getArgument(0);
                long accepted = Math.min(request.getStackSize(), Long.MAX_VALUE - amount.get());
                if (call.getArgument(1) == Actionable.MODULATE) amount.addAndGet(accepted);
                return accepted == request.getStackSize() ? null : new FluxStack(request.getStackSize() - accepted);
            });
            when(inventory.extractItems(any(FluxStack.class), any(Actionable.class), any(IActionSource.class))).thenAnswer(call -> {
                FluxStack request = call.getArgument(0);
                long moved = Math.min(request.getStackSize(), amount.get());
                if (call.getArgument(1) == Actionable.MODULATE) amount.addAndGet(-moved);
                return moved == 0 ? null : new FluxStack(moved);
            });
        }
    }
}
