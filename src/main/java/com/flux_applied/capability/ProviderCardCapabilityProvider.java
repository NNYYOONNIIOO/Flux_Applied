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
import java.util.EnumMap;

/**
 * Dynamically provides CapabilityEnergy for interfaces with Provider Cards.
 * Attached via AttachCapabilitiesEvent to TileCableBus, TileInterface, TileFluidInterface,
 * and ae2fcr/mekeng block-form interfaces (TileDualInterface, TileTrioInterface, TileGasInterface).
 */
public class ProviderCardCapabilityProvider implements ICapabilityProvider {

    private static final long CARD_CACHE_TICKS = 20L;

    private final TileEntity te;
    private final EnumMap<EnumFacing, ItemStack> panelCards = new EnumMap<>(EnumFacing.class);
    private ItemStack blockCard = ItemStack.EMPTY;
    private long cardCacheTick = Long.MIN_VALUE;
    private boolean cardCacheValid;

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
                return getProviderCard(facing) != null;
            }

            // Block form: any interface TE that has a provider card
            return getProviderCard(null) != null;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (!hasCapability(capability, facing)) return null;

        try {
            // Panel form
            if (te instanceof TileCableBus) {
                IPart part = ((IPartHost) te).getPart(AEPartLocation.fromFacing(facing));
                ItemStack card = getProviderCard(facing);
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

    /**
     * Capability lookups can happen many times per tick for a cable bus, even
     * when no Flux Applied card is installed. Cache the negative lookup as
     * well as the positive one and allow at most a one-second refresh delay
     * after a card is inserted or removed.
     */
    @Nullable
    private ItemStack getProviderCard(@Nullable EnumFacing facing) {
        long now = this.te.getWorld() == null ? Long.MIN_VALUE : this.te.getWorld().getTotalWorldTime();
        if (!this.cardCacheValid || now == Long.MIN_VALUE
                || now < this.cardCacheTick || now - this.cardCacheTick >= CARD_CACHE_TICKS) {
            refreshCardCache(now);
        }

        if (this.te instanceof TileCableBus) {
            return facing == null ? null : this.panelCards.get(facing);
        }
        return this.blockCard.isEmpty() ? null : this.blockCard;
    }

    private void refreshCardCache(long now) {
        this.panelCards.clear();
        this.blockCard = ItemStack.EMPTY;

        if (this.te instanceof TileCableBus) {
            IPartHost partHost = (IPartHost) this.te;
            for (EnumFacing facing : EnumFacing.values()) {
                IPart part = partHost.getPart(AEPartLocation.fromFacing(facing));
                if (part != null && ProviderCardHelper.isInterfacePart(part)) {
                    ItemStack card = ProviderCardHelper.findProviderCard(part);
                    if (card != null) {
                        this.panelCards.put(facing, card);
                    }
                }
            }
        } else {
            ItemStack card = ProviderCardHelper.findProviderCardInBlockTE(this.te);
            if (card != null) {
                this.blockCard = card;
            }
        }

        this.cardCacheTick = now;
        this.cardCacheValid = true;
    }
}
