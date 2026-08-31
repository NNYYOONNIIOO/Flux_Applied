package com.flux_applied.handler;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.fluids.tile.TileFluidInterface;
import appeng.tile.misc.TileInterface;
import com.flux_applied.ModConfig;
import com.flux_applied.integration.MekanismCeuAeUpgradeIntegration;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.EnergyTransferHelper;
import com.flux_applied.util.ProviderCardHelper;
import com.flux_applied.util.ReflectionCache;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Processes one interface from its own AE2 grid tick callback.
 *
 * AE2 invokes tickingRequest only for the grid tickable that is already
 * active in the network. There is no world-level or interface-list scan here.
 */
public final class FluxInterfaceGridTickHandler {

    public static final FluxInterfaceGridTickHandler INSTANCE = new FluxInterfaceGridTickHandler();

    private final MekanismCeuAeUpgradeIntegration mekanismIntegration =
            new MekanismCeuAeUpgradeIntegration();
    private final Map<Object, ItemStack> cardCache = new WeakHashMap<>();
    private final Map<Object, Boolean> cardCacheKnown = new WeakHashMap<>();

    private FluxInterfaceGridTickHandler() {
    }

    /**
     * Inventory mixins call this only when an upgrade slot changes. A negative
     * result is cached too, so normal interfaces do not rescan their upgrade
     * inventories on every AE2 grid tick.
     */
    public void invalidateCardCache(Object target) {
        if (target == null) {
            return;
        }
        this.cardCacheKnown.remove(target);
        this.cardCache.remove(target);
    }

    public void onTileEntityTick(TileEntity tile) {
        if (tile == null || tile.isInvalid() || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        if ((tile instanceof TileInterface || tile instanceof TileFluidInterface)
                || isOptionalBlockInterface(tile)) {
            processBlockInterface(tile);
        }
    }

    public void onPartTick(IPart part) {
        if (part == null || !ProviderCardHelper.isInterfacePart(part)) {
            return;
        }

        ItemStack card = getCachedCard(part);
        if (card == null) {
            return;
        }

        IGridNode node = ProviderCardHelper.getGridNode(part);
        if (!isActive(node)) {
            return;
        }

        TileEntity host = findPartHost(part);
        EnumFacing facing = findPartFacing(part, host);
        if (host == null || facing == null || host.getWorld() == null || host.getWorld().isRemote) {
            return;
        }

        TileEntity neighbor = host.getWorld().getTileEntity(host.getPos().offset(facing));
        int mode = ItemProviderCard.getMode(card);
        if (mode == 1 || mode == 2) {
            autoEjectEnergy(part, neighbor, facing);
        }
        if (mode == 0 || mode == 2) {
            autoPullEnergy(part, neighbor, facing);
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

    private void processBlockInterface(TileEntity tile) {
        ItemStack card = getCachedCard(tile);
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

    private boolean isOptionalBlockInterface(TileEntity tile) {
        String className = tile.getClass().getName();
        return className.contains("TileDualInterface")
                || className.contains("TileTrioInterface")
                || className.contains("TileGasInterface");
    }

    private ItemStack getCachedCard(Object target) {
        if (target == null) {
            return null;
        }
        if (this.cardCacheKnown.containsKey(target)) {
            return this.cardCache.get(target);
        }

        ItemStack card = null;
        if (target instanceof IPart) {
            card = ProviderCardHelper.findProviderCard((IPart) target);
        } else if (target instanceof TileEntity) {
            card = ProviderCardHelper.findProviderCardInBlockTE((TileEntity) target);
        }

        this.cardCacheKnown.put(target, Boolean.TRUE);
        if (card != null) {
            this.cardCache.put(target, card);
        }
        return card;
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

    private TileEntity findPartHost(IPart part) {
        Object tile = invokeNoArg(part, "getTileEntity");
        if (tile instanceof TileEntity) {
            return (TileEntity) tile;
        }
        Object host = invokeNoArg(part, "getHost");
        if (host instanceof IPartHost) {
            return ((IPartHost) host).getTile();
        }
        Object hostTile = invokeNoArg(host, "getTile");
        return hostTile instanceof TileEntity ? (TileEntity) hostTile : null;
    }

    private EnumFacing findPartFacing(IPart part, TileEntity host) {
        if (host instanceof IPartHost) {
            IPartHost partHost = (IPartHost) host;
            for (EnumFacing facing : EnumFacing.values()) {
                if (partHost.getPart(facing) == part) {
                    return facing;
                }
            }
        }

        Object side = invokeNoArg(part, "getSide");
        if (side instanceof EnumFacing) {
            return (EnumFacing) side;
        }
        Object facing = invokeNoArg(side, "getFacing");
        return facing instanceof EnumFacing ? (EnumFacing) facing : null;
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        return ReflectionCache.invokeMethod(
                ReflectionCache.getMethod(target.getClass(), methodName), target);
    }
}
