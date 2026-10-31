package com.flux_applied.integration;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.helpers.MachineSource;
import com.flux_applied.ModConfig;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.util.EnergyTransferHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class MekanismCeuAeUpgradeIntegration {

    private static final String UPGRADE_HOST = "mekceuaeupgrade.common.host.IAEUpgradeHost";
    private static final int SLEEP_TICKS = 100;
    private static final int UPGRADE_SCAN_INTERVAL = 100;
    private final Set<TileEntity> candidates =
            Collections.newSetFromMap(new WeakHashMap<TileEntity, Boolean>());
    private final Set<TileEntity> activeMachines =
            Collections.newSetFromMap(new IdentityHashMap<TileEntity, Boolean>());
    private final Map<TileEntity, IEnergyStorage> energyCache = new IdentityHashMap<>();
    private final Map<TileEntity, Long> nextQuery = new IdentityHashMap<>();
    private Method hasAEUpgrade;
    private Class<?> upgradeHostClass;
    private boolean integrationChecked;
    private boolean integrationAvailable;
    private final Set<Integer> discoveredWorlds = new HashSet<>();

    /**
     * Register possible integration hosts when their tile entity is created.
     * This replaces the previous full-world scan in onWorldTick.
     */
    public void onTileEntityAttached(TileEntity tile) {
        if (tile == null || !isIntegrationAvailable() || this.upgradeHostClass == null) return;
        if (this.upgradeHostClass.isInstance(tile)) {
            this.candidates.add(tile);
        }
    }

    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END || !ModConfig.INTEGRATION.enableMekanismCeuAeUpgrade) return;
        if (!isIntegrationAvailable()) return;

        long now = event.world.getTotalWorldTime();
        if (now % UPGRADE_SCAN_INTERVAL == 0L) {
            refreshActiveMachines(event, now);
        }

        if (this.activeMachines.isEmpty()) return;
        for (TileEntity tile : new ArrayList<>(this.activeMachines)) {
            if (tile == null) {
                this.activeMachines.remove(null);
                continue;
            }
            if (tile.getWorld() != event.world) continue;
            if (tile.isInvalid()
                    || !event.world.isBlockLoaded(tile.getPos())
                    || !(tile instanceof IActionHost)) {
                this.activeMachines.remove(tile);
                this.energyCache.remove(tile);
                this.nextQuery.remove(tile);
                continue;
            }

            IEnergyStorage energy = this.energyCache.get(tile);
            if (energy == null) {
                energy = findEnergy(tile);
                if (energy == null) continue;
                this.energyCache.put(tile, energy);
            }
            if (energy == null || energy.getEnergyStored() >= energy.getMaxEnergyStored()) continue;

            long stored = energy.getEnergyStored();
            if (stored < 0) {
                this.energyCache.remove(tile);
                this.activeMachines.remove(tile);
                continue;
            }
            Long scheduled = nextQuery.get(tile);
            if (stored <= 0 && scheduled != null && now < scheduled) continue;

            long moved = pullEnergy((IActionHost) tile, energy);
            if (moved <= 0) nextQuery.put(tile, now + SLEEP_TICKS);
            else nextQuery.remove(tile);
        }
        nextQuery.keySet().removeIf(tile -> tile == null || tile.isInvalid() || tile.getWorld() == null
                || !tile.getWorld().isBlockLoaded(tile.getPos()));
    }

    private void refreshActiveMachines(TickEvent.WorldTickEvent event, long now) {
        if (this.discoveredWorlds.add(event.world.provider.getDimension())) {
            discoverLoadedMachines(event);
        }

        for (TileEntity tile : new ArrayList<>(this.candidates)) {
            if (tile == null) {
                this.candidates.remove(null);
                continue;
            }
            if (tile.getWorld() != event.world) continue;
            if (tile.isInvalid()
                    || !event.world.isBlockLoaded(tile.getPos())
                    || !(tile instanceof IActionHost)) {
                this.candidates.remove(tile);
                this.activeMachines.remove(tile);
                this.energyCache.remove(tile);
                this.nextQuery.remove(tile);
                continue;
            }

            if (hasUpgrade(tile)) {
                this.activeMachines.add(tile);
            } else {
                this.activeMachines.remove(tile);
                this.energyCache.remove(tile);
                this.nextQuery.remove(tile);
            }
        }

        // Drop stale active entries even if the candidate weak reference has
        // already been collected.
        for (TileEntity tile : new ArrayList<>(this.activeMachines)) {
            if (!this.candidates.contains(tile)) {
                this.activeMachines.remove(tile);
                this.energyCache.remove(tile);
                this.nextQuery.remove(tile);
            }
        }
    }

    private void discoverLoadedMachines(TickEvent.WorldTickEvent event) {
        for (TileEntity tile : new ArrayList<>(event.world.loadedTileEntityList)) {
            if (this.upgradeHostClass.isInstance(tile)) {
                this.candidates.add(tile);
            }
        }
    }

    private boolean isIntegrationAvailable() {
        if (!integrationChecked) {
            integrationChecked = true;
            integrationAvailable = Loader.isModLoaded("mekceuaeupgrade") && initReflection();
        }
        return integrationAvailable;
    }

    private boolean initReflection() {
        try {
            upgradeHostClass = Class.forName(UPGRADE_HOST);
            hasAEUpgrade = upgradeHostClass.getMethod("hasAEUpgrade");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasUpgrade(TileEntity tile) {
        try {
            return (Boolean) hasAEUpgrade.invoke(tile);
        } catch (Exception ignored) {
            return false;
        }
    }

    private IEnergyStorage findEnergy(TileEntity tile) {
        for (EnumFacing side : EnumFacing.values()) {
            IEnergyStorage storage = tile.getCapability(CapabilityEnergy.ENERGY, side);
            if (storage != null) return storage;
        }
        return tile.getCapability(CapabilityEnergy.ENERGY, null);
    }

    private long pullEnergy(IActionHost host, IEnergyStorage machine) {
        try {
            IGridNode node = host.getActionableNode();
            if (node == null || !node.isActive()) return 0;
            IGrid grid = node.getGrid();
            if (grid == null) return 0;
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage == null) return 0;

            long limit = Math.min(ModConfig.getMekanismCeuAeUpgradeExtractRate(),
                    ModConfig.getEnergyPortTransferRate());
            long capacity = (long) machine.getMaxEnergyStored() - machine.getEnergyStored();
            long request = Math.min(limit, Math.max(0, capacity));
            if (request <= 0) return 0;

            MachineSource source = new MachineSource(host);
            FluxStack simulated = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(new FluxStack(request), Actionable.SIMULATE, source);
            long available = simulated == null ? 0 : Math.min(request, simulated.getStackSize());
            if (available <= 0) return 0;

            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(new FluxStack(available), Actionable.MODULATE, source);
            long actual = extracted == null ? 0 : extracted.getStackSize();
            if (actual <= 0) return 0;
            long received = EnergyTransferHelper.receive(machine, actual);
            if (received < actual) {
                storage.getInventory(FluxStorageChannel.INSTANCE)
                        .injectItems(new FluxStack(actual - received), Actionable.MODULATE, source);
            }
            return received;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
