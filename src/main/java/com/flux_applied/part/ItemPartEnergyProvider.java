package com.flux_applied.part;
import appeng.items.parts.PartItem;
import com.flux_applied.FluxApplied;
public final class ItemPartEnergyProvider extends PartItem<PartEnergyProvider> {
    public ItemPartEnergyProvider() { super(FluxApplied.properties(), PartEnergyProvider::new); }
}
