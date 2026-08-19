package com.flux_applied.part;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.BusSupport;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.PartBasicState;
import appeng.parts.PartModel;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.util.EnergyTransferHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.io.IOException;

public class PartEnergyProvider extends PartBasicState implements IGridTickable, IEnergyStorage {

    private static final ResourceLocation MODEL_BASE = new ResourceLocation(FluxApplied.MODID, "part/energy_provider");
    private static final ResourceLocation MODEL_INPUT = new ResourceLocation(FluxApplied.MODID, "part/energy_provider_input");
    private static final ResourceLocation MODEL_OUTPUT = new ResourceLocation(FluxApplied.MODID, "part/energy_provider_output");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE);
    public static final IPartModel MODELS_POWERED = new PartModel(MODEL_BASE);
    public static final IPartModel MODELS_INPUT = new PartModel(MODEL_INPUT);
    public static final IPartModel MODELS_OUTPUT = new PartModel(MODEL_OUTPUT);

    private static final long MAX_ENERGY = Long.MAX_VALUE;
    private static final long DEFAULT_MAX_TRANSFER = ModConfig.getEnergyPortTransferRate();

    // IC2 conversion: 1 EU = 4 FE
    private static final double FE_PER_EU = 4.0;
    private static final double EU_PER_FE = 0.25;

    private final MachineSource actionSource;
    private int mode = 2;
    private long maxTransfer = DEFAULT_MAX_TRANSFER;
    private boolean firstTick = true;

    public PartEnergyProvider(ItemStack is) {
        super(is);
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.actionSource = new MachineSource(this);
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeByte((byte) mode);
    }

    @Override
    public boolean readFromStream(ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        int oldMode = this.mode;
        this.mode = data.readByte();
        return changed || oldMode != this.mode;
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Mode", mode);
        data.setLong("MaxTransfer", maxTransfer);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        mode = data.getInteger("Mode");
        if (mode < 0 || mode > 2) mode = 2;
        maxTransfer = data.getLong("MaxTransfer");
        if (maxTransfer <= 0) maxTransfer = DEFAULT_MAX_TRANSFER;
    }

    @Override
    public boolean canBePlacedOn(BusSupport what) {
        return what == BusSupport.CABLE;
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.GLASS;
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            switch (mode) {
                case 0: return MODELS_INPUT;
                case 1: return MODELS_OUTPUT;
                default: return MODELS_POWERED;
            }
        } else if (this.isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (!getHost().getTile().getWorld().isRemote) {
            toggleMode(player);
        }
        return true;
    }

    public void toggleMode(EntityPlayer player) {
        mode = (mode + 1) % 3;
        saveChanges();
        getHost().markForUpdate();

        String modeKey;
        switch (mode) {
            case 0: modeKey = "flux_applied.message.input_only"; break;
            case 1: modeKey = "flux_applied.message.output_only"; break;
            default: modeKey = "flux_applied.message.bidirectional"; break;
        }
        player.sendStatusMessage(new TextComponentTranslation(modeKey), true);
    }

    @Override
    public boolean hasCapability(Capability<?> capabilityClass) {
        return capabilityClass == CapabilityEnergy.ENERGY;
    }

    @Override
    public <T> T getCapability(Capability<T> capabilityClass) {
        if (capabilityClass == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return null;
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (firstTick) {
            firstTick = false;
        }

        if (isActive()) {
            if (mode == 1) {
                autoEjectEnergy();
            }
            // IC2 interaction for part form
            if (Loader.isModLoaded("ic2")) {
                try {
                    if (mode == 1 || mode == 2) {
                        ic2EjectEnergy();
                    }
                    if (mode == 0 || mode == 2) {
                        ic2PullEnergy();
                    }
                } catch (NoClassDefFoundError ignored) {
                }
            }
        }

        return TickRateModulation.SLOWER;
    }

    // ===== IC2 interaction for Part form =====

    private void ic2EjectEnergy() {
        TileEntity hostTile = getHost().getTile();
        if (hostTile == null || hostTile.getWorld() == null) return;

        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);

            EnumFacing facing = getSide().getFacing();
            ic2.api.energy.tile.IEnergyTile ic2Tile = ic2.api.energy.EnergyNet.instance.getSubTile(hostTile.getWorld(), hostTile.getPos().offset(facing));
            if (ic2Tile instanceof ic2.api.energy.tile.IEnergySink) {
                ic2.api.energy.tile.IEnergySink sink = (ic2.api.energy.tile.IEnergySink) ic2Tile;
                double demanded = sink.getDemandedEnergy();
                if (demanded > 0) {
                    FluxStack request = new FluxStack(Long.MAX_VALUE);
                    FluxStack available = storage.getInventory(FluxStorageChannel.INSTANCE)
                            .extractItems(request, Actionable.SIMULATE, actionSource);
                    long availableFE = available != null ? available.getStackSize() : 0;
                    if (availableFE <= 0) return;

                    double euToProvide = Math.min(demanded, Math.min(availableFE * EU_PER_FE, maxTransfer * EU_PER_FE));
                    double leftover = sink.injectEnergy(facing.getOpposite(), euToProvide, 0);
                    double euProvided = euToProvide - leftover;
                    long feProvided = (long) (euProvided * FE_PER_EU);
                    if (feProvided > 0) {
                        storage.getInventory(FluxStorageChannel.INSTANCE)
                                .extractItems(new FluxStack(feProvided), Actionable.MODULATE, actionSource);
                    }
                }
            }
        } catch (GridAccessException ignored) {
        }
    }

    private void ic2PullEnergy() {
        TileEntity hostTile = getHost().getTile();
        if (hostTile == null || hostTile.getWorld() == null) return;

        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);

            EnumFacing facing = getSide().getFacing();
            ic2.api.energy.tile.IEnergyTile ic2Tile = ic2.api.energy.EnergyNet.instance.getSubTile(hostTile.getWorld(), hostTile.getPos().offset(facing));
            if (ic2Tile instanceof ic2.api.energy.tile.IEnergySource) {
                ic2.api.energy.tile.IEnergySource source = (ic2.api.energy.tile.IEnergySource) ic2Tile;
                double offered = source.getOfferedEnergy();
                if (offered > 0) {
                    double euToDraw = Math.min(offered, maxTransfer * EU_PER_FE);
                    source.drawEnergy(euToDraw);
                    long feReceived = (long) (euToDraw * FE_PER_EU);
                    if (feReceived > 0) {
                        storage.getInventory(FluxStorageChannel.INSTANCE)
                                .injectItems(new FluxStack(feReceived), Actionable.MODULATE, actionSource);
                    }
                }
            }
        } catch (GridAccessException ignored) {
        }
    }

    // ===== Forge Energy =====

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (mode == 1) return 0;
        if (!isActive()) return 0;

        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            FluxStack toInsert = new FluxStack(Math.min((long) maxReceive, maxTransfer));
            FluxStack remaining = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .injectItems(toInsert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
            return (int) (toInsert.getStackSize() - (remaining != null ? remaining.getStackSize() : 0));
        } catch (GridAccessException e) {
            return 0;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (mode == 0) return 0;
        if (!isActive()) return 0;

        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            FluxStack request = new FluxStack(Math.min((long) maxExtract, maxTransfer));
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
            return extracted != null ? (int) Math.min(extracted.getStackSize(), Integer.MAX_VALUE) : 0;
        } catch (GridAccessException e) {
            return 0;
        }
    }

    @Override
    public int getEnergyStored() {
        if (!isActive()) return 0;
        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            FluxStack request = new FluxStack(Long.MAX_VALUE);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, Actionable.SIMULATE, actionSource);
            return extracted != null ? (int) Math.min(extracted.getStackSize(), Integer.MAX_VALUE) : 0;
        } catch (GridAccessException e) {
            return 0;
        }
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return (mode == 1 || mode == 2) && isActive();
    }

    @Override
    public boolean canReceive() {
        return (mode == 0 || mode == 2) && isActive();
    }

    public int getMode() {
        return mode;
    }

    public long getNetworkEnergyStored() {
        if (!isActive()) return 0;
        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            FluxStack request = new FluxStack(Long.MAX_VALUE);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, Actionable.SIMULATE, actionSource);
            return extracted != null ? extracted.getStackSize() : 0;
        } catch (GridAccessException e) {
            return 0;
        }
    }

    private void autoEjectEnergy() {
        TileEntity hostTile = getHost().getTile();
        if (hostTile == null || hostTile.getWorld() == null) return;

        try {
            IGrid grid = getProxy().getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);

            EnumFacing facing = getSide().getFacing();
            TileEntity neighbor = hostTile.getWorld().getTileEntity(hostTile.getPos().offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborStorage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborStorage != null && neighborStorage.canReceive()) {
                    FluxStack request = new FluxStack(Long.MAX_VALUE);
                    FluxStack available = storage.getInventory(FluxStorageChannel.INSTANCE)
                            .extractItems(request, Actionable.SIMULATE, actionSource);
                    long availableFE = available != null ? available.getStackSize() : 0;
                    if (availableFE <= 0) return;

                    long toTransfer = Math.min(availableFE, maxTransfer);
                    long received = EnergyTransferHelper.receive(neighborStorage, toTransfer);
                    if (received > 0) {
                        storage.getInventory(FluxStorageChannel.INSTANCE)
                                .extractItems(new FluxStack(received), Actionable.MODULATE, actionSource);
                    }
                }
            }
        } catch (GridAccessException ignored) {
        }
    }

}
