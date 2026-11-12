package com.flux_applied.capability;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import javax.annotation.Nullable;
import java.util.EnumMap;

/** Capability queries inspect only the requested side; upgrade events refresh card presence. */
public final class ProviderCardCapabilityProvider implements ICapabilityProvider {
    private final TileEntity tile;
    private final EnumMap<Direction, IPart> parts = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> capabilities = new EnumMap<>(Direction.class);
    private LazyOptional<IEnergyStorage> blockCapability;
    public ProviderCardCapabilityProvider(TileEntity tile) { this.tile = tile; }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability != CapabilityEnergy.ENERGY || tile.isRemoved()) return LazyOptional.empty();
        if (tile instanceof IPartHost) {
            if (side == null) return LazyOptional.empty();
            IPart part = ((IPartHost) tile).getPart(AEPartLocation.fromFacing(side));
            if (parts.get(side) != part) {
                LazyOptional<IEnergyStorage> old = capabilities.remove(side);
                if (old != null) old.invalidate();
                if (part == null) parts.remove(side); else parts.put(side, part);
            }
            InterfaceEnergyState state = ProviderCardHelper.state(part);
            if (state == null || !state.hasCard()) return LazyOptional.empty();
            return capabilities.computeIfAbsent(side, ignored -> LazyOptional.of(state::storage)).cast();
        }
        InterfaceEnergyState state = ProviderCardHelper.state(tile);
        if (state == null || !state.hasCard()) return LazyOptional.empty();
        if (blockCapability == null) blockCapability = LazyOptional.of(state::storage);
        return blockCapability.cast();
    }
    public void invalidate() {
        capabilities.values().forEach(LazyOptional::invalidate);
        capabilities.clear();
        parts.clear();
        if (blockCapability != null) blockCapability.invalidate();
        blockCapability = null;
    }
}
