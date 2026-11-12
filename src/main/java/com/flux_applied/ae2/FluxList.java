package com.flux_applied.ae2;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IItemList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
public final class FluxList implements IItemList<FluxStack> {
    private final FluxStack stack = new FluxStack(0);
    @Override public void add(FluxStack value) { stack.add(value); }
    @Override public void addStorage(FluxStack value) { if (value != null) stack.incStackSize(value.getStackSize()); }
    @Override public void addCrafting(FluxStack value) { if (value != null) stack.setCraftable(true); }
    @Override public void addRequestable(FluxStack value) { if (value != null) stack.incCountRequestable(value.getCountRequestable()); }
    @Override public FluxStack findPrecise(FluxStack value) { return value != null && stack.isMeaningful() ? stack : null; }
    @Override public Collection<FluxStack> findFuzzy(FluxStack value, FuzzyMode mode) { return isEmpty() ? Collections.emptyList() : Collections.singletonList(stack); }
    @Override public int size() { return stack.isMeaningful() ? 1 : 0; }
    @Override public FluxStack getFirstItem() { return isEmpty() ? null : stack; }
    @Override public boolean isEmpty() { return !stack.isMeaningful(); }
    @Override public void resetStatus() { stack.reset(); }
    @Override public Iterator<FluxStack> iterator() { return isEmpty() ? Collections.emptyIterator() : Collections.singletonList(stack).iterator(); }
}
