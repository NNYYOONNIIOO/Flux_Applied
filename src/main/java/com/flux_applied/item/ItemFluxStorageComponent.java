package com.flux_applied.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemFluxStorageComponent extends Item
{
    private final int kilobytes;

    public ItemFluxStorageComponent(int kilobytes) {
        this.kilobytes = kilobytes;
        this.setMaxStackSize(64);
        this.setHasSubtypes(false);
    }

    public int getKilobytes() {
        return this.kilobytes;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            subItems.add(new ItemStack(this));
        }
    }
}