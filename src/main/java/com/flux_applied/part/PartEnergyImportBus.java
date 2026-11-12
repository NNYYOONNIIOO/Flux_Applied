package com.flux_applied.part;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEInventory;
import appeng.api.util.AECableType;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.BasicStatePart;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
public class PartEnergyImportBus extends FluxPart implements IGridTickable {

    private static final ResourceLocation MODEL_BASE = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus");
    private static final ResourceLocation MODEL_OFF = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus_off");
    private static final ResourceLocation MODEL_HAS_CHANNEL = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus_has_channel");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_HAS_CHANNEL);

    private final MachineSource actionSource;
    private long transferRate;
    private final com.flux_applied.energy.NetworkEnergyStorage network = new com.flux_applied.energy.NetworkEnergyStorage(
            () -> this, () -> 0, () -> getTile() != null && !getTile().isRemoved(), this::getTransferRate,
            () -> getTile() == null || getTile().getWorld() == null ? 0 : getTile().getWorld().getGameTime());

    public PartEnergyImportBus(ItemStack is) {
        super(is);
        this.actionSource = new MachineSource(this);
        this.transferRate = ModConfig.getEnergyImportBusInitialInputExtractRate();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(6, 6, 11, 10, 10, 13);
        bch.addBox(5, 5, 13, 11, 11, 14);
        bch.addBox(4, 4, 14, 12, 12, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 5;
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putLong("TransferRate", transferRate);
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        transferRate = data.contains("TransferRate")
                ? data.getLong("TransferRate")
                : ModConfig.getEnergyImportBusInitialInputExtractRate();
        normalizeTransferRate();
    }

    @Override
    protected CompoundNBT downloadSettings(SettingsFrom from, CompoundNBT output) {
        super.downloadSettings(from, output);
        output.putLong("TransferRate", transferRate);
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, CompoundNBT compound, PlayerEntity player) {
        super.uploadSettings(from, compound, player);
        if (compound.contains("TransferRate")) {
            transferRate = compound.getLong("TransferRate");
            normalizeTransferRate();
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    private void normalizeTransferRate() {
        long maximum = ModConfig.getEnergyImportBusMaximumInputExtractRate();
        if (transferRate <= 0) {
            transferRate = ModConfig.getEnergyImportBusInitialInputExtractRate();
        }
        if (transferRate > maximum) {
            transferRate = maximum;
        }
    }

    public long getTransferRate() {
        normalizeTransferRate();
        return transferRate;
    }

    public long getMaximumTransferRate() {
        return ModConfig.getEnergyImportBusMaximumInputExtractRate();
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        return useFluxMemoryCard(player, hand);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!getProxy().isActive()) {
            return TickRateModulation.IDLE;
        }

        boolean worked = pullEnergyIntoNetwork();
        growTransferRate();
        return worked ? TickRateModulation.SAME : TickRateModulation.SLOWER;
    }

    private void growTransferRate() {
        normalizeTransferRate();
        if (!ModConfig.isEnergyImportBusRateGrowthEnabled()) {
            return;
        }

        long maximum = ModConfig.getEnergyImportBusMaximumInputExtractRate();
        long increment = ModConfig.getEnergyImportBusRateGrowthPerTick();
        if (transferRate >= maximum || increment <= 0) {
            return;
        }

        long remaining = maximum - transferRate;
        transferRate += Math.min(remaining, increment);
        getHost().markForSave();
    }

    private boolean pullEnergyIntoNetwork() {
        IEnergyStorage target = getTargetEnergyStorage();
        if (target == null || !target.canExtract()) {
            return false;
        }

        return com.flux_applied.util.EnergyTransferHelper.push(target, network, getTransferRate()) > 0;
    }

    private long getStackSize(@Nullable FluxStack stack) {
        return stack == null ? 0L : Math.max(0L, stack.getStackSize());
    }

    @Nullable
    private IEnergyStorage getTargetEnergyStorage() {
        if (getHost() == null || getHost().getTile() == null || getSide() == null) {
            return null;
        }

        TileEntity hostTile = getHost().getTile();
        BlockPos targetPos = hostTile.getPos().offset(getSide().getFacing());
        if (hostTile.getWorld() == null
                || !hostTile.getWorld().isBlockLoaded(targetPos)) {
            return null;
        }

        TileEntity target = hostTile.getWorld().getTileEntity(targetPos);
        if (target == null) {
            return null;
        }

        Direction facing = getSide().getFacing();
        return target.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).orElse(null);
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }
}
