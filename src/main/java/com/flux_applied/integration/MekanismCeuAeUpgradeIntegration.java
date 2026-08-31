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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class MekanismCeuAeUpgradeIntegration {

    private static final String UPGRADE_HOST = "mekceuaeupgrade.common.host.IAEUpgradeHost";
    private static final int SLEEP_TICKS = 100;
    private static final long UPGRADE_CACHE_TICKS = 20L;
    private final Set<TileEntity> candidates =
            Collections.newSetFromMap(new WeakHashMap<TileEntity, Boolean>());
    private final Map<TileEntity, Long> nextQuery = new IdentityHashMap<>();
    private final Map<TileEntity, UpgradeCache> upgradeCache = new IdentityHashMap<>();
    private Method hasAEUpgrade;
    private Class<?> upgradeHostClass;
    private boolean initialized;
    private boolean integrationChecked;
    private boolean integrationAvailable;

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
        for (TileEntity tile : this.candidates) {
            if (tile == null || tile.isInvalid() || tile.getWorld() != event.world
                    || !event.world.isBlockLoaded(tile.getPos())
                    || !(tile instanceof IActionHost) || !hasUpgrade(tile, now)) continue;
            IEnergyStorage energy = findEnergy(tile);
            if (energy == null || energy.getEnergyStored() >= energy.getMaxEnergyStored()) continue;

            long stored = energy.getEnergyStored();
            Long scheduled = nextQuery.get(tile);
            if (stored <= 0 && scheduled != null && now < scheduled) continue;

            long moved = pullEnergy((IActionHost) tile, energy);
            if (moved <= 0) nextQuery.put(tile, now + SLEEP_TICKS);
            else nextQuery.remove(tile);
        }
        nextQuery.keySet().removeIf(tile -> tile == null || tile.isInvalid() || tile.getWorld() == null
                || !tile.getWorld().isBlockLoaded(tile.getPos()));
        upgradeCache.keySet().removeIf(tile -> tile == null || tile.isInvalid() || tile.getWorld() == null
                || !tile.getWorld().isBlockLoaded(tile.getPos()));
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
            initialized = true;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasUpgrade(TileEntity tile, long now) {
        UpgradeCache cached = upgradeCache.get(tile);
        if (cached != null && now < cached.nextCheck) {
            return cached.hasUpgrade;
        }

        boolean result;
        try {
            result = (Boolean) hasAEUpgrade.invoke(tile);
        } catch (Exception ignored) {
            result = false;
        }
        upgradeCache.put(tile, new UpgradeCache(result, now + UPGRADE_CACHE_TICKS));
        return result;
    }

    private static final class UpgradeCache {
        private final boolean hasUpgrade;
        private final long nextCheck;

        private UpgradeCache(boolean hasUpgrade, long nextCheck) {
            this.hasUpgrade = hasUpgrade;
            this.nextCheck = nextCheck;
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
