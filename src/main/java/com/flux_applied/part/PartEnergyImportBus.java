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
import appeng.parts.PartBasicState;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
public class PartEnergyImportBus extends PartBasicState implements IGridTickable {

    private static final ResourceLocation MODEL_BASE = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus");
    private static final ResourceLocation MODEL_OFF = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus_off");
    private static final ResourceLocation MODEL_HAS_CHANNEL = new ResourceLocation(FluxApplied.MODID, "part/energy_import_bus_has_channel");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_HAS_CHANNEL);

    private final MachineSource actionSource;
    private long transferRate;

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
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setLong("TransferRate", transferRate);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        transferRate = data.hasKey("TransferRate")
                ? data.getLong("TransferRate")
                : ModConfig.getEnergyImportBusInitialInputExtractRate();
        normalizeTransferRate();
    }

    @Override
    protected NBTTagCompound downloadSettings(SettingsFrom from, NBTTagCompound output) {
        super.downloadSettings(from, output);
        output.setLong("TransferRate", transferRate);
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound compound, EntityPlayer player) {
        super.uploadSettings(from, compound, player);
        if (compound.hasKey("TransferRate")) {
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
    public boolean onPartActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
        return false;
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

        try {
            IStorageGrid storage = getProxy().getStorage();
            IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);
            if (inventory == null) {
                return false;
            }

            long remaining = getTransferRate();
            boolean worked = false;
            while (remaining > 0) {
                int request = (int) Math.min(remaining, Integer.MAX_VALUE);
                int available = target.extractEnergy(request, true);
                if (available <= 0) {
                    break;
                }

                FluxStack simulatedRemainder = inventory.injectItems(
                        new FluxStack(available), Actionable.SIMULATE, actionSource);
                long insertable = available - getStackSize(simulatedRemainder);
                if (insertable <= 0) {
                    break;
                }

                int extracted = target.extractEnergy((int) Math.min(insertable, Integer.MAX_VALUE), false);
                if (extracted <= 0) {
                    break;
                }

                FluxStack remainder = inventory.injectItems(
                        new FluxStack(extracted), Actionable.MODULATE, actionSource);
                long inserted = extracted - getStackSize(remainder);
                if (inserted > 0) {
                    worked = true;
                }
                remaining -= extracted;

            }
            return worked;
        } catch (GridAccessException ignored) {
            return false;
        }
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
                || hostTile.getWorld().getChunkProvider().getLoadedChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4) == null) {
            return null;
        }

        TileEntity target = hostTile.getWorld().getTileEntity(targetPos);
        if (target == null) {
            return null;
        }

        EnumFacing facing = getSide().getFacing();
        return target.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
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
