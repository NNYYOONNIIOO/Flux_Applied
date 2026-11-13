package com.flux_applied.integration.jade;

import appeng.fluids.tile.FluidInterfaceTileEntity;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.tile.networking.CableBusTileEntity;
import com.flux_applied.tile.TileEntityEnergyProvider;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;

@WailaPlugin("jade")
public final class FluxJadePlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        EnergyInfoProvider provider = new EnergyInfoProvider();
        for (Class<?> type : new Class<?>[] {
                TileEntityEnergyProvider.class, CableBusTileEntity.class,
                InterfaceTileEntity.class, FluidInterfaceTileEntity.class }) {
            registrar.registerBlockDataProvider(provider, type);
            registrar.registerComponentProvider(provider, TooltipPosition.BODY, type);
        }
    }
}
