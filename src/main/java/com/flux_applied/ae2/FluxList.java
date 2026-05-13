package com.flux_applied.ae2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IItemList;

public class FluxList implements IItemList<FluxStack> {

    private long feAmount = 0;

    @Override
    public void add(FluxStack option) {
        if (option != null) {
            this.feAmount += option.getStackSize();
        }
    }

    @Override
    public void addStorage(FluxStack option) {
        this.add(option);
    }

    @Override
    public void addCrafting(FluxStack option) {
    }

    @Override
    public void addRequestable(FluxStack option) {
    }

    @Override
    public FluxStack findPrecise(FluxStack i) {
        if (this.feAmount > 0) {
            return new FluxStack(this.feAmount);
        }
        return null;
    }

    @Override
    public Collection<FluxStack> findFuzzy(FluxStack input, FuzzyMode fuzzy) {
        Collection<FluxStack> result = new ArrayList<>();
        if (this.feAmount > 0) {
            result.add(new FluxStack(this.feAmount));
        }
        return result;
    }

    @Override
    public int size() {
        return this.feAmount > 0 ? 1 : 0;
    }

    @Override
    public FluxStack getFirstItem() {
        if (this.feAmount > 0) {
            return new FluxStack(this.feAmount);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return this.feAmount <= 0;
    }

    @Override
    public void resetStatus() {
        this.feAmount = 0;
    }

    @Nonnull
    @Override
    public Iterator<FluxStack> iterator() {
        return new Iterator<FluxStack>() {
            private boolean hasNext = feAmount > 0;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public FluxStack next() {
                if (hasNext) {
                    hasNext = false;
                    return new FluxStack(feAmount);
                }
                return null;
            }
        };
    }
}