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
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.integration.MekanismCeuAeUpgradeIntegration;
import com.flux_applied.util.EnergyTransferHelper;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class FluxInterfaceTickHandler {

    private int tickCounter = 0;
    private final MekanismCeuAeUpgradeIntegration mekanismCeuAeUpgradeIntegration = new MekanismCeuAeUpgradeIntegration();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter < 1) return;
        tickCounter = 0;

        World world = event.world;
        mekanismCeuAeUpgradeIntegration.onWorldTick(event);

        List<TileEntity> tileEntities = new ArrayList<>(world.loadedTileEntityList);
        for (TileEntity te : tileEntities) {
            if (te.isInvalid()) continue;

            // Panel form: TileCableBus
            if (te instanceof TileCableBus) {
                TileCableBus cableBus = (TileCableBus) te;
                IPartHost partHost = cableBus;

                for (EnumFacing facing : EnumFacing.values()) {
                    IPart part = partHost.getPart(facing);
                    if (part == null) continue;
                    if (!ProviderCardHelper.isInterfacePart(part)) continue;

                    ItemStack card = ProviderCardHelper.findProviderCard(part);
                    if (card == null) continue;

                    int mode = ItemProviderCard.getMode(card);

                    if (ProviderCardHelper.getGridNode(part) == null) continue;
                    try {
                        if (ProviderCardHelper.getGridNode(part).getGrid() == null || !ProviderCardHelper.getGridNode(part).isActive()) continue;
                    } catch (Exception e) {
                        continue;
                    }

                    EnumFacing partFacing = facing;
                    TileEntity neighborTE = world.getTileEntity(te.getPos().offset(partFacing));

                    if (mode == 1 || mode == 2) {
                        autoEjectEnergy(part, neighborTE, partFacing);
                    }
                    if (mode == 0 || mode == 2) {
                        autoPullEnergy(part, neighborTE, partFacing);
                    }
                }
            }

            // Block form: TileInterface / TileFluidInterface / ae2fcr TileDualInterface,TileTrioInterface / mekeng TileGasInterface
            if (isBlockFormInterface(te)) {
                if (te instanceof IPartHost) continue; // skip panel form

                ItemStack card = ProviderCardHelper.findProviderCardInBlockTE(te);
                if (card == null) continue;

                int mode = ItemProviderCard.getMode(card);

                // Check network connection
                if (!(te instanceof IActionHost)) continue;
                IGridNode node;
                IGrid grid;
                try {
                    node = ((IActionHost) te).getActionableNode();
                    if (node == null) continue;
                    grid = node.getGrid();
                    if (grid == null || !node.isActive()) continue;
                } catch (Exception e) {
                    continue;
                }

                // Iterate all sides for block form
                for (EnumFacing facing : EnumFacing.values()) {
                    TileEntity neighborTE = world.getTileEntity(te.getPos().offset(facing));
                    if (neighborTE == null) continue;

                    if (mode == 1 || mode == 2) {
                        autoEjectEnergyBlock((IActionHost) te, neighborTE, facing);
                    }
                    if (mode == 0 || mode == 2) {
                        autoPullEnergyBlock((IActionHost) te, neighborTE, facing);
                    }
                }
            }
        }
    }

    private void autoEjectEnergy(IPart part, TileEntity neighborTE, EnumFacing facing) {
        if (neighborTE == null) return;
        IEnergyStorage neighborStorage = neighborTE.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (neighborStorage == null || !neighborStorage.canReceive()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        long available = ProviderCardHelper.extractEnergy(part, maxTransfer, true);
        if (available <= 0) return;

        long toTransfer = Math.min(available, maxTransfer);
        long accepted = EnergyTransferHelper.receive(neighborStorage, toTransfer);
        if (accepted > 0) {
            ProviderCardHelper.extractEnergy(part, accepted, false);
        }
    }

    private void autoPullEnergy(IPart part, TileEntity neighborTE, EnumFacing facing) {
        if (neighborTE == null) return;
        IEnergyStorage neighborStorage = neighborTE.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (neighborStorage == null || !neighborStorage.canExtract()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        long notInserted = ProviderCardHelper.injectEnergy(part, maxTransfer, true);
        long canAccept = maxTransfer - notInserted;
        if (canAccept <= 0) return;

        long toPull = Math.min(maxTransfer, canAccept);
        long pulled = EnergyTransferHelper.extract(neighborStorage, toPull);
        if (pulled > 0) {
            ProviderCardHelper.injectEnergy(part, pulled, false);
        }
    }

    private void autoEjectEnergyBlock(IActionHost actionHost, TileEntity neighborTE, EnumFacing facing) {
        if (neighborTE == null) return;
        IEnergyStorage neighborStorage = neighborTE.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (neighborStorage == null || !neighborStorage.canReceive()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        // Extract from network via the wrapper logic
        try {
            IGridNode node = actionHost.getActionableNode();
            if (node == null) return;
            IGrid grid = node.getGrid();
            if (grid == null) return;
            appeng.api.networking.storage.IStorageGrid storage = grid.getCache(appeng.api.networking.storage.IStorageGrid.class);
            if (storage == null) return;

            appeng.me.helpers.MachineSource source = new appeng.me.helpers.MachineSource(actionHost);
            com.flux_applied.ae2.FluxStack request = new com.flux_applied.ae2.FluxStack(maxTransfer);
            com.flux_applied.ae2.FluxStack extracted = storage.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                    .extractItems(request, appeng.api.config.Actionable.SIMULATE, source);
            if (extracted == null || extracted.getStackSize() <= 0) return;

            long toTransfer = Math.min(extracted.getStackSize(), maxTransfer);
            long accepted = EnergyTransferHelper.receive(neighborStorage, toTransfer);
            if (accepted > 0) {
                com.flux_applied.ae2.FluxStack realRequest = new com.flux_applied.ae2.FluxStack(accepted);
                storage.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                        .extractItems(realRequest, appeng.api.config.Actionable.MODULATE, source);
            }
        } catch (Exception ignored) {}
    }

    private void autoPullEnergyBlock(IActionHost actionHost, TileEntity neighborTE, EnumFacing facing) {
        if (neighborTE == null) return;
        IEnergyStorage neighborStorage = neighborTE.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
        if (neighborStorage == null || !neighborStorage.canExtract()) return;

        long maxTransfer = ModConfig.getEnergyPortTransferRate();
        if (maxTransfer <= 0) return;

        try {
            IGridNode node = actionHost.getActionableNode();
            if (node == null) return;
            IGrid grid = node.getGrid();
            if (grid == null) return;
            appeng.api.networking.storage.IStorageGrid storage = grid.getCache(appeng.api.networking.storage.IStorageGrid.class);
            if (storage == null) return;

            appeng.me.helpers.MachineSource source = new appeng.me.helpers.MachineSource(actionHost);
            com.flux_applied.ae2.FluxStack toInsert = new com.flux_applied.ae2.FluxStack(maxTransfer);
            com.flux_applied.ae2.FluxStack remaining = storage.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                    .injectItems(toInsert, appeng.api.config.Actionable.SIMULATE, source);

            long canAccept = remaining != null ? maxTransfer - remaining.getStackSize() : maxTransfer;
            if (canAccept <= 0) return;

            long toPull = Math.min(maxTransfer, canAccept);
            long pulled = EnergyTransferHelper.extract(neighborStorage, toPull);
            if (pulled > 0) {
                com.flux_applied.ae2.FluxStack realInsert = new com.flux_applied.ae2.FluxStack(pulled);
                storage.getInventory(com.flux_applied.ae2.FluxStorageChannel.INSTANCE)
                        .injectItems(realInsert, appeng.api.config.Actionable.MODULATE, source);
            }
        } catch (Exception ignored) {}
    }

    private boolean isBlockFormInterface(TileEntity te) {
        if (te instanceof TileInterface || te instanceof TileFluidInterface) return true;
        String className = te.getClass().getName();
        return className.contains("TileDualInterface") || className.contains("TileTrioInterface") || className.contains("TileGasInterface");
    }
}
