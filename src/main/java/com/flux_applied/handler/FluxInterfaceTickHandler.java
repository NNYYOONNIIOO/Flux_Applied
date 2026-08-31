package com.flux_applied.handler;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.fluids.tile.TileFluidInterface;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import com.flux_applied.ModConfig;
import com.flux_applied.integration.MekanismCeuAeUpgradeIntegration;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.EnergyTransferHelper;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Processes a single interface from its own update method.
 *
 * The previous implementation listened to every world tick and walked all
 * loaded interface-shaped tile entities. The Mixin entry point now calls
 * onTileEntityTick for the tile that Minecraft is already updating, so there
 * is no additional world-level interface scan.
 */
public final class FluxInterfaceTickHandler {

    public static final FluxInterfaceTickHandler INSTANCE = new FluxInterfaceTickHandler();

    private final MekanismCeuAeUpgradeIntegration mekanismIntegration =
            new MekanismCeuAeUpgradeIntegration();

    private FluxInterfaceTickHandler() {
    }

    public void onTileEntityTick(TileEntity tile) {
        if (tile == null || tile.isInvalid() || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }

        if (tile instanceof TileCableBus) {
            processPanelInterfaces((TileCableBus) tile);
        } else if (isBlockFormInterface(tile) && !(tile instanceof IPartHost)) {
            processBlockInterface(tile);
        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        this.mekanismIntegration.onTileEntityAttached(event.getObject());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        this.mekanismIntegration.onWorldTick(event);
    }

    private void processPanelInterfaces(TileCableBus cableBus) {
        IPartHost partHost = cableBus;
        for (EnumFacing facing : EnumFacing.values()) {
            IPart part = partHost.getPart(facing);
            if (part == null || !ProviderCardHelper.isInterfacePart(part)) {
                continue;
            }

            ItemStack card = ProviderCardHelper.findProviderCard(part);
            if (card == null) {
                continue;
            }

            IGridNode node = ProviderCardHelper.getGridNode(part);
            if (!isActive(node)) {
                continue;
            }

            int mode = ItemProviderCard.getMode(card);
            TileEntity neighbor = cableBus.getWorld().getTileEntity(cableBus.getPos().offset(facing));
            if (mode == 1 || mode == 2) {
                autoEjectEnergy(part, neighbor, facing);
            }
            if (mode == 0 || mode == 2) {
                autoPullEnergy(part, neighbor, facing);
            }
        }
    }

    private void processBlockInterface(TileEntity tile) {
        ItemStack card = ProviderCardHelper.findProviderCardInBlockTE(tile);
        if (card == null || !(tile instanceof IActionHost)) {
            return;
        }

        IActionHost actionHost = (IActionHost) tile;
        if (!isActive(actionHost.getActionableNode())) {
            return;
        }

        int mode = ItemProviderCard.getMode(card);
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity neighbor = tile.getWorld().getTileEntity(tile.getPos().offset(facing));
            if (neighbor == null) {
                continue;
            }

            if (mode == 1 || mode == 2) {
                autoEjectEnergyBlock(actionHost, neighbor, facing);
            }
            if (mode == 0 || mode == 2) {
                autoPullEnergyBlock(actionHost, neighbor, facing);
            }
        }
    }

    private boolean isActive(IGridNode node) {
        if (node == null) {
            return false;
        }
        try {
            return node.isActive() && node.getGrid() != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void autoEjectEnergy(IPart part, TileEntity neighbor, EnumFacing facing) {
        if (neighbor == null) return;
        IEnergyStorage storage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (storage == null || !storage.canReceive()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        long available = ProviderCardHelper.extractEnergy(part, maxTransfer, true);
        if (available <= 0) return;

        long accepted = EnergyTransferHelper.receive(storage, Math.min(available, maxTransfer));
        if (accepted > 0) {
            ProviderCardHelper.extractEnergy(part, accepted, false);
        }
    }

    private void autoPullEnergy(IPart part, TileEntity neighbor, EnumFacing facing) {
        if (neighbor == null) return;
        IEnergyStorage storage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (storage == null || !storage.canExtract()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        long notInserted = ProviderCardHelper.injectEnergy(part, maxTransfer, true);
        long canAccept = maxTransfer - notInserted;
        if (canAccept <= 0) return;

        long pulled = EnergyTransferHelper.extract(storage, Math.min(maxTransfer, canAccept));
        if (pulled > 0) {
            ProviderCardHelper.injectEnergy(part, pulled, false);
        }
    }

    private void autoEjectEnergyBlock(IActionHost actionHost, TileEntity neighbor, EnumFacing facing) {
        IEnergyStorage storage = getEnergyStorage(neighbor, facing, true);
        if (storage == null) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        try {
            IGridNode node = actionHost.getActionableNode();
            if (!isActive(node)) return;
            IGrid grid = node.getGrid();
            appeng.api.networking.storage.IStorageGrid network =
                    grid.getCache(appeng.api.networking.storage.IStorageGrid.class);
            if (network == null) return;

            appeng.me.helpers.MachineSource source = new appeng.me.helpers.MachineSource(actionHost);
            com.flux_applied.ae2.FluxStack extracted = network
                    .getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                    .extractItems(new com.flux_applied.ae2.FluxStack(maxTransfer),
                            appeng.api.config.Actionable.SIMULATE, source);
            if (extracted == null || extracted.getStackSize() <= 0) return;

            long accepted = EnergyTransferHelper.receive(storage,
                    Math.min(extracted.getStackSize(), maxTransfer));
            if (accepted > 0) {
                network.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                        .extractItems(new com.flux_applied.ae2.FluxStack(accepted),
                                appeng.api.config.Actionable.MODULATE, source);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void autoPullEnergyBlock(IActionHost actionHost, TileEntity neighbor, EnumFacing facing) {
        IEnergyStorage storage = getEnergyStorage(neighbor, facing, false);
        if (storage == null) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        try {
            IGridNode node = actionHost.getActionableNode();
            if (!isActive(node)) return;
            IGrid grid = node.getGrid();
            appeng.api.networking.storage.IStorageGrid network =
                    grid.getCache(appeng.api.networking.storage.IStorageGrid.class);
            if (network == null) return;

            appeng.me.helpers.MachineSource source = new appeng.me.helpers.MachineSource(actionHost);
            com.flux_applied.ae2.FluxStack remaining = network
                    .getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                    .injectItems(new com.flux_applied.ae2.FluxStack(maxTransfer),
                            appeng.api.config.Actionable.SIMULATE, source);
            long canAccept = remaining == null ? maxTransfer : maxTransfer - remaining.getStackSize();
            if (canAccept <= 0) return;

            long pulled = EnergyTransferHelper.extract(storage, Math.min(maxTransfer, canAccept));
            if (pulled > 0) {
                network.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                        .injectItems(new com.flux_applied.ae2.FluxStack(pulled),
                                appeng.api.config.Actionable.MODULATE, source);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private IEnergyStorage getEnergyStorage(TileEntity neighbor, EnumFacing facing, boolean receive) {
        if (neighbor == null) return null;
        IEnergyStorage storage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (storage == null) return null;
        if (receive ? !storage.canReceive() : !storage.canExtract()) return null;
        return storage;
    }

    private boolean isBlockFormInterface(TileEntity tile) {
        if (tile instanceof TileInterface || tile instanceof TileFluidInterface) {
            return true;
        }
        String className = tile.getClass().getName();
        return className.contains("TileDualInterface")
                || className.contains("TileTrioInterface")
                || className.contains("TileGasInterface");
    }
}
