package com.flux_applied.handler;

import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import com.flux_applied.ModConfig;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.energy.NetworkEnergyStorage;
import com.flux_applied.util.EnergyTransferHelper;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/** Called by AE2 only for this host; there is no world or interface-list scan. */
public final class FluxInterfaceGridTickHandler {
    public static final FluxInterfaceGridTickHandler INSTANCE = new FluxInterfaceGridTickHandler();
    private FluxInterfaceGridTickHandler() {}
    public void onTileEntityTick(TileEntity tile) { tick(tile); }
    public void onPartTick(IPart part) { tick(part); }
    public boolean tick(Object host) {
        InterfaceEnergyState state = ProviderCardHelper.state(host);
        if (state == null || !state.hasCard() || !(host instanceof IActionHost)) return false;
        TileEntity tile = ProviderCardHelper.tile(host);
        if (tile == null || tile.isRemoved() || tile.getWorld() == null || tile.getWorld().isRemote) return false;
        NetworkEnergyStorage storage = state.storage();
        if (storage.inventory() == null) return false;
        boolean worked = false;
        for (Direction side : ProviderCardHelper.targets(host)) {
            BlockPos pos = tile.getPos().offset(side);
            if (!tile.getWorld().isBlockLoaded(pos)) continue;
            TileEntity neighbor = tile.getWorld().getTileEntity(pos);
            if (neighbor == null) continue;
            IEnergyStorage target = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite()).orElse(null);
            if (target == null) continue;
            if (EnergyTransferHelper.sameNetwork(storage, target)) continue;
            long limit = ModConfig.getEnergyPortTransferRate();
            if (storage.canExtract()) worked |= EnergyTransferHelper.push(storage, target, limit) > 0;
            if (storage.canReceive()) worked |= EnergyTransferHelper.push(target, storage, limit) > 0;
        }
        return worked;
    }
}
