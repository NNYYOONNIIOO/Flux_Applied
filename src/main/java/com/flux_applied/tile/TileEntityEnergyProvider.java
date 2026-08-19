package com.flux_applied.tile;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.ModConfig;
import com.flux_applied.util.EnergyTransferHelper;

public class TileEntityEnergyProvider extends TileEntity implements ITickable, IEnergyStorage, IGridProxyable, IActionHost
{
    public static final long MAX_ENERGY = Long.MAX_VALUE;
    public static final long DEFAULT_MAX_TRANSFER = ModConfig.getEnergyPortTransferRate();

    private long energy = 0L;
    private int mode = 2; // 0=input only, 1=output only, 2=bidirectional (default)
    private long maxTransfer = DEFAULT_MAX_TRANSFER;

    private AENetworkProxy gridProxy;
    private MachineSource actionSource;
    private boolean firstTick = true;
    private boolean isNetworkConnected = false;
    private int lastNetworkCheckTick = 0;
    private int ticks = 0;

    // IC2 registration (used by mixin)
    private boolean ic2Registered = false;

    @Override
    public AENetworkProxy getProxy() {
        if (this.gridProxy == null) {
            this.gridProxy = new AENetworkProxy(this, "proxy", new ItemStack(com.flux_applied.FluxApplied.energyPortBlock), true);
            this.gridProxy.setValidSides(java.util.EnumSet.allOf(EnumFacing.class));
            this.gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            this.actionSource = new MachineSource(this);
        }
        return this.gridProxy;
    }

    @Override
    public void validate() {
        super.validate();
        this.getProxy().validate();
    }

    private void onReady() {
        this.getProxy().onReady();
    }

    @Override
    public void update() {
        if (!world.isRemote && firstTick) {
            firstTick = false;
            onReady();

            if (energy > 0) {
                transferLocalEnergyToNetwork(energy);
                energy = 0;
                markDirty();
            }
        }

        ticks++;

        if (!world.isRemote && ticks - lastNetworkCheckTick >= 20) {
            lastNetworkCheckTick = ticks;
            boolean wasConnected = isNetworkConnected;
            isNetworkConnected = checkNetworkConnection();

            if (wasConnected != isNetworkConnected) {
                markDirty();
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

                if (isNetworkConnected && energy > 0) {
                    transferLocalEnergyToNetwork(energy);
                    energy = 0;
                    markDirty();
                } else if (!isNetworkConnected) {
                    long networkEnergy = getEnergyFromNetwork();
                    if (networkEnergy > 0) {
                        energy = Math.min(networkEnergy, MAX_ENERGY);
                        extractEnergyFromNetwork(networkEnergy);
                        markDirty();
                    }
                }
            } else if (isNetworkConnected && ticks % 60 == 0) {
                long networkEnergy = getEnergyFromNetwork();
                if (networkEnergy != energy) {
                    energy = networkEnergy;
                    markDirty();
                }
            }
        }

        // Auto-eject for output-only mode
        if (!world.isRemote && mode == 1 && isNetworkConnected && ticks % 10 == 0) {
            autoEjectEnergy();
        }
    }

    // ===== Forge Energy =====

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!isNetworkConnected || mode == 1) {
            return 0;
        }

        long toReceive = Math.min((long) maxReceive, maxTransfer);

        if (isNetworkConnected) {
            return transferEnergyToNetwork(toReceive, simulate);
        } else {
            toReceive = Math.min(MAX_ENERGY - energy, toReceive);
            int received = (int) Math.min(toReceive, Integer.MAX_VALUE);

            if (!simulate) {
                energy += received;
                markDirty();
            }

            return received;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!isNetworkConnected || mode == 0) {
            return 0;
        }

        long toExtract = Math.min((long) maxExtract, maxTransfer);

        if (isNetworkConnected) {
            return extractEnergyFromNetwork(toExtract, simulate);
        } else {
            toExtract = Math.min(energy, toExtract);
            int extracted = (int) Math.min(toExtract, Integer.MAX_VALUE);

            if (!simulate) {
                energy -= extracted;
                markDirty();
            }

            return extracted;
        }
    }

    @Override
    public int getEnergyStored() {
        if (isNetworkConnected && !world.isRemote) {
            long networkEnergy = getEnergyFromNetwork();
            return (int) Math.min(networkEnergy, Integer.MAX_VALUE);
        }
        return (int) Math.min(energy, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    public long getEnergyStoredLong() {
        if (isNetworkConnected && !world.isRemote) {
            return getEnergyFromNetwork();
        }
        return energy;
    }

    public long getMaxEnergyStoredLong() {
        return MAX_ENERGY;
    }

    public long getMaxTransfer() {
        return maxTransfer;
    }

    public void setMaxTransfer(long transferRate) {
        this.maxTransfer = Math.max(1, Math.min(transferRate, Long.MAX_VALUE));
        markDirty();
    }

    @Override
    public boolean canExtract() {
        return (mode == 1 || mode == 2) && isNetworkConnected;
    }

    @Override
    public boolean canReceive() {
        return (mode == 0 || mode == 2) && isNetworkConnected;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energy = compound.getLong("Energy");
        mode = compound.getInteger("Mode");
        isNetworkConnected = compound.getBoolean("NetworkConnected");
        maxTransfer = compound.getLong("MaxTransfer");
        if (maxTransfer <= 0 || maxTransfer == Integer.MAX_VALUE) {
            maxTransfer = ModConfig.getEnergyPortTransferRate();
        }
        this.getProxy().readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong("Energy", energy);
        compound.setInteger("Mode", mode);
        compound.setBoolean("NetworkConnected", isNetworkConnected);
        compound.setLong("MaxTransfer", maxTransfer);
        if (this.gridProxy != null) {
            this.gridProxy.writeToNBT(compound);
        }
        return compound;
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, -1, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
        int oldMode = this.mode;
        this.readFromNBT(pkt.getNbtCompound());
        if (oldMode != this.mode && this.world != null && this.world.isRemote) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this.gridProxy != null) {
            this.gridProxy.invalidate();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (this.gridProxy != null) {
            this.gridProxy.onChunkUnload();
        }
    }

    public void toggleMode(EntityPlayer player) {
        mode = (mode + 1) % 3;
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        String modeKey;
        switch (mode) {
            case 0:
                modeKey = "flux_applied.message.input_only";
                break;
            case 1:
                modeKey = "flux_applied.message.output_only";
                break;
            default:
                modeKey = "flux_applied.message.bidirectional";
                break;
        }
        player.sendStatusMessage(new TextComponentTranslation(modeKey), true);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return this.getProxy().getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void gridChanged() {
    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return this.getProxy().getNode();
    }

    @Override
    public void securityBreak() {
        this.world.destroyBlock(this.pos, true);
    }

    private boolean checkNetworkConnection() {
        if (gridProxy == null || gridProxy.getNode() == null) {
            return false;
        }
        IGridNode node = gridProxy.getNode();
        return node.isActive() && node.getGrid() != null;
    }

    public int getMode() {
        return mode;
    }

    public boolean isNetworkConnected() {
        return isNetworkConnected;
    }

    private int transferEnergyToNetwork(long amount, boolean simulate) {
        try {
            if (gridProxy == null || gridProxy.getNode() == null) {
                return 0;
            }
            IGridNode node = gridProxy.getNode();
            if (!node.isActive() || node.getGrid() == null) {
                return 0;
            }

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);

            FluxStack toInsert = new FluxStack(amount);
            FluxStack remaining = inventory.injectItems(toInsert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);

            int transferred = (int) (amount - (remaining != null ? remaining.getStackSize() : 0));
            return transferred;
        } catch (Exception e) {
            return 0;
        }
    }

    private int extractEnergyFromNetwork(long amount, boolean simulate) {
        try {
            if (gridProxy == null || gridProxy.getNode() == null) {
                return 0;
            }
            IGridNode node = gridProxy.getNode();
            if (!node.isActive() || node.getGrid() == null) {
                return 0;
            }

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);

            FluxStack request = new FluxStack(amount);
            FluxStack extracted = inventory.extractItems(request, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);

            return extracted != null ? (int) Math.min(extracted.getStackSize(), Integer.MAX_VALUE) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long getEnergyFromNetwork() {
        try {
            if (gridProxy == null || gridProxy.getNode() == null) {
                return 0;
            }
            IGridNode node = gridProxy.getNode();
            if (!node.isActive() || node.getGrid() != null) {
                IGrid grid = node.getGrid();
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);

                FluxStack request = new FluxStack(Long.MAX_VALUE);
                FluxStack extracted = inventory.extractItems(request, Actionable.SIMULATE, actionSource);

                return extracted != null ? extracted.getStackSize() : 0;
            }
            return 0;
        } catch (Exception e) {
            return energy;
        }
    }

    private void transferLocalEnergyToNetwork(long amount) {
        try {
            if (gridProxy == null || gridProxy.getNode() == null) {
                return;
            }
            IGridNode node = gridProxy.getNode();
            if (!node.isActive() || node.getGrid() == null) {
                return;
            }

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);

            FluxStack toInsert = new FluxStack(amount);
            inventory.injectItems(toInsert, Actionable.MODULATE, actionSource);
        } catch (Exception e) {
            // Ignore errors during network transfer
        }
    }

    private void extractEnergyFromNetwork(long amount) {
        try {
            if (gridProxy == null || gridProxy.getNode() == null) {
                return;
            }
            IGridNode node = gridProxy.getNode();
            if (!node.isActive() || node.getGrid() == null) {
                return;
            }

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<FluxStack> inventory = storage.getInventory(FluxStorageChannel.INSTANCE);

            FluxStack request = new FluxStack(amount);
            inventory.extractItems(request, Actionable.MODULATE, actionSource);
        } catch (Exception e) {
            // Ignore errors during network extraction
        }
    }

    private void autoEjectEnergy() {
        if (!isNetworkConnected || mode != 1) {
            return;
        }

        long networkEnergy = getEnergyFromNetwork();
        if (networkEnergy <= 0) {
            return;
        }

        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage storage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (storage != null && storage.canReceive()) {
                    long toTransfer = Math.min(networkEnergy, maxTransfer);
                    long received = EnergyTransferHelper.receive(storage, toTransfer);

                    if (received > 0) {
                        extractEnergyFromNetwork(received);
                        networkEnergy -= received;

                        if (networkEnergy <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
