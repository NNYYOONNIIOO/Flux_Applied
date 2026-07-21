package com.flux_applied.part;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import com.flux_applied.FluxApplied;
import com.flux_applied.FluxAppliedTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ItemPartEnergyImportBus extends Item implements IPartItem<PartEnergyImportBus> {

    public ItemPartEnergyImportBus() {
        setRegistryName(FluxApplied.MODID, "energy_import_bus");
        setUnlocalizedName("flux_applied.energy_import_bus");
        setCreativeTab(FluxAppliedTab.INSTANCE);
        setMaxStackSize(64);
    }

    @Nullable
    @Override
    public PartEnergyImportBus createPartFromItemStack(ItemStack is) {
        return new PartEnergyImportBus(is);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing side, float hitX, float hitY, float hitZ) {
        return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
    }
}
