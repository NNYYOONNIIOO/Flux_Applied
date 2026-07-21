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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

public class MekanismCeuAeUpgradeIntegration {

    private static final String UPGRADE_HOST = "mekceuaeupgrade.common.host.IAEUpgradeHost";
    private static final int SLEEP_TICKS = 100;
    private final Map<TileEntity, Long> nextQuery = new IdentityHashMap<>();
    private Method hasAEUpgrade;
    private boolean initialized;

    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END || !ModConfig.INTEGRATION.enableMekanismCeuAeUpgrade) return;
        if (!Loader.isModLoaded("mekceuaeupgrade")) return;
        if (!initialized && !initReflection()) return;

        long now = event.world.getTotalWorldTime();
        for (TileEntity tile : event.world.loadedTileEntityList) {
            if (tile == null || tile.isInvalid() || !(tile instanceof IActionHost) || !hasUpgrade(tile)) continue;
            IEnergyStorage energy = findEnergy(tile);
            if (energy == null || energy.getEnergyStored() >= energy.getMaxEnergyStored()) continue;

            long stored = energy.getEnergyStored();
            Long scheduled = nextQuery.get(tile);
            if (stored <= 0 && scheduled != null && now < scheduled) continue;

            long moved = pullEnergy((IActionHost) tile, energy);
            if (moved <= 0) nextQuery.put(tile, now + SLEEP_TICKS);
            else nextQuery.remove(tile);
        }
        nextQuery.keySet().removeIf(tile -> tile.isInvalid() || tile.getWorld() != event.world);
    }

    private boolean initReflection() {
        try {
            hasAEUpgrade = Class.forName(UPGRADE_HOST).getMethod("hasAEUpgrade");
            initialized = true;
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

            int accepted = machine.receiveEnergy((int) Math.min(request, Integer.MAX_VALUE), true);
            if (accepted <= 0) return 0;

            MachineSource source = new MachineSource(host);
            FluxStack simulated = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(new FluxStack(accepted), Actionable.SIMULATE, source);
            long available = simulated == null ? 0 : Math.min(accepted, simulated.getStackSize());
            if (available <= 0) return 0;

            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(new FluxStack(available), Actionable.MODULATE, source);
            long actual = extracted == null ? 0 : extracted.getStackSize();
            if (actual <= 0) return 0;
            return machine.receiveEnergy((int) Math.min(actual, Integer.MAX_VALUE), false);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
