package com.flux_applied.item;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
public class OptionalFluxItem extends Item {
    private final boolean housing;
    public OptionalFluxItem(boolean housing) { super(FluxApplied.properties()); this.housing = housing; }
    @Override public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if ((housing ? ModConfig.CELL_HOUSING.get() : ModConfig.STORAGE_COMPONENTS.get())) super.fillItemGroup(group, items);
    }
}
