package com.flux_applied;

import net.minecraft.util.text.translation.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class FluxAppliedTab extends CreativeTabs
{
    public static final FluxAppliedTab INSTANCE = new FluxAppliedTab();

    public FluxAppliedTab() {
        super("flux_applied_tab");
    }

    @Override
    public ItemStack getTabIconItem() {
        return new ItemStack(FluxApplied.fluxStorageCell1k);
    }

    @Override
    public String getTranslatedTabLabel() {
        return I18n.translateToLocal("itemGroup.flux_applied");
    }
}