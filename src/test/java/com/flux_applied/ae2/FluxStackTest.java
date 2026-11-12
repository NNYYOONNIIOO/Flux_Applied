package com.flux_applied.ae2;

import org.junit.Test;
import static org.junit.Assert.*;

public class FluxStackTest {
    @Test public void storageListAggregatesLongEnergyWithoutOverflow() {
        FluxList list = new FluxList();
        list.addStorage(new FluxStack(Long.MAX_VALUE - 10));
        list.addStorage(new FluxStack(100));
        assertEquals(Long.MAX_VALUE, list.getFirstItem().getStackSize());
    }
    @Test public void signedNetworkDeltasCanRemoveAllStoredEnergy() {
        FluxList list = new FluxList();
        list.addStorage(new FluxStack(Long.MAX_VALUE));
        list.addStorage(new FluxStack(-Long.MAX_VALUE));
        assertTrue(list.isEmpty());
    }
    @Test public void copiesDoNotShareQuantityOrCraftingState() {
        FluxStack source = new FluxStack(4000000000L).setCraftable(true).setCountRequestable(7);
        FluxStack copy = source.copy();
        copy.setStackSize(1).setCraftable(false);
        assertEquals(4000000000L, source.getStackSize());
        assertTrue(source.isCraftable());
        assertEquals(7, copy.getCountRequestable());
    }
    @Test public void quantitiesHaveOneIdentityAndResetClearsTheList() {
        assertEquals(new FluxStack(1), new FluxStack(Long.MAX_VALUE));
        FluxList list = new FluxList();
        list.addCrafting(new FluxStack(1));
        assertFalse(list.isEmpty());
        list.resetStatus();
        assertTrue(list.isEmpty());
    }
}
