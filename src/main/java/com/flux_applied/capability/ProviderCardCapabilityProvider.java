package com.flux_applied.capability;

import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.fluids.tile.TileFluidInterface;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import com.flux_applied.FluxApplied;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.energy.BlockInterfaceEnergyWrapper;
import com.flux_applied.energy.InterfaceEnergyWrapper;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Dynamically provides CapabilityEnergy for interfaces with Provider Cards.
 * Attached via AttachCapabilitiesEvent to TileCableBus, TileInterface, TileFluidInterface,
 * and ae2fcr/mekeng block-form interfaces (TileDualInterface, TileTrioInterface, TileGasInterface).
 */
public class ProviderCardCapabilityProvider implements ICapabilityProvider {

    private final TileEntity te;

    public ProviderCardCapabilityProvider(TileEntity te) {
        this.te = te;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability != CapabilityEnergy.ENERGY) return false;
        if (te.isInvalid()) return false;

        try {
            // Panel form: TileCableBus
            if (te instanceof TileCableBus) {
                if (facing == null) return false;
                IPart part = ((IPartHost) te).getPart(AEPartLocation.fromFacing(facing));
                if (part == null || !ProviderCardHelper.isInterfacePart(part)) return false;
                return ProviderCardHelper.findProviderCard(part) != null;
            }

            // Block form: any interface TE that has a provider card
            ItemStack card = ProviderCardHelper.findProviderCardInBlockTE(te);
            if (card != null) return true;
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (!hasCapability(capability, facing)) return null;

        try {
            // Panel form
            if (te instanceof TileCableBus) {
                IPart part = ((IPartHost) te).getPart(AEPartLocation.fromFacing(facing));
                ItemStack card = ProviderCardHelper.findProviderCard(part);
                if (card != null) {
                    int mode = ItemProviderCard.getMode(card);
                    return CapabilityEnergy.ENERGY.cast(new InterfaceEnergyWrapper(part, mode));
                }
            }

            // Block form
            if (te instanceof IActionHost) {
                return CapabilityEnergy.ENERGY.cast(new BlockInterfaceEnergyWrapper((IActionHost) te));
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }
}
