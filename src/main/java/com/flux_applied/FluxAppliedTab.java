package com.flux_applied;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
public final class FluxAppliedTab extends ItemGroup {
    public static final FluxAppliedTab INSTANCE = new FluxAppliedTab();
    private FluxAppliedTab() { super("flux_applied"); }
    @Override public ItemStack createIcon() { return new ItemStack(FluxApplied.energyPortItem); }
}
