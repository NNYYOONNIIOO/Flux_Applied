package com.flux_applied.part;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.parts.PartBasicState;
import appeng.parts.PartModel;
import com.flux_applied.FluxApplied;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
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
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PartEnergyStorageBus extends PartBasicState implements IGridTickable, ICellContainer {

    public static final int MODE_INPUT = 0;
    public static final int MODE_OUTPUT = 1;
    public static final int MODE_BIDIRECTIONAL = 2;

    private static final ResourceLocation MODEL = new ResourceLocation(FluxApplied.MODID, "part/energy_storage_bus");
    public static final IPartModel MODELS = new PartModel(MODEL);

    private int mode = MODE_BIDIRECTIONAL;
    private long lastReportedEnergy = -1L;
    private final IMEInventoryHandler<FluxStack> handler = new EnergyStorageHandler();

    public PartEnergyStorageBus(ItemStack is) {
        super(is);
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(3, 3, 15, 13, 13, 16);
        bch.addBox(2, 2, 14, 14, 14, 15);
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
        int oldMode = mode;
        mode = data.readByte();
        if (mode < MODE_INPUT || mode > MODE_BIDIRECTIONAL) {
            mode = MODE_BIDIRECTIONAL;
        }
        return changed || oldMode != mode;
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Mode", mode);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        mode = data.hasKey("Mode") ? data.getInteger("Mode") : MODE_BIDIRECTIONAL;
        if (mode < MODE_INPUT || mode > MODE_BIDIRECTIONAL) {
            mode = MODE_BIDIRECTIONAL;
        }
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return MODELS;
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (!player.getHeldItem(hand).isEmpty()) {
            return false;
        }
        if (!getHost().getTile().getWorld().isRemote) {
            toggleMode(player);
        }
        return true;
    }

    public void toggleMode(EntityPlayer player) {
        mode = (mode + 1) % 3;
        lastReportedEnergy = -1L;
        saveChanges();
        getHost().markForUpdate();
        notifyCellArrayUpdate();
        player.sendStatusMessage(new TextComponentTranslation(getModeMessageKey()), true);
    }

    private String getModeMessageKey() {
        switch (mode) {
            case MODE_INPUT:
                return "flux_applied.message.storage_bus_input_only";
            case MODE_OUTPUT:
                return "flux_applied.message.storage_bus_output_only";
            default:
                return "flux_applied.message.storage_bus_bidirectional";
        }
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!getHost().getTile().getWorld().isRemote && getProxy().isActive()) {
            long currentEnergy = getExtractableEnergy();
            if (currentEnergy != lastReportedEnergy) {
                lastReportedEnergy = currentEnergy;
                notifyCellArrayUpdate();
            }
        }
        return TickRateModulation.SLOWER;
    }

    private void notifyCellArrayUpdate() {
        try {
            if (getProxy().isActive()) {
                getProxy().getGrid().postEvent(new MENetworkCellArrayUpdate());
            }
        } catch (GridAccessException ignored) {
        }
    }

    @Nullable
    private IEnergyStorage getTargetEnergyStorage() {
        if (getHost() == null || getHost().getTile() == null || getSide() == null) {
            return null;
        }
        TileEntity hostTile = getHost().getTile();
        EnumFacing facing = getSide().getFacing();
        TileEntity target = hostTile.getWorld().getTileEntity(hostTile.getPos().offset(facing));
        if (target == null) {
            return null;
        }
        return target.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
    }

    private boolean canReadTarget() {
        return mode == MODE_INPUT || mode == MODE_BIDIRECTIONAL;
    }

    private boolean canWriteTarget() {
        return mode == MODE_OUTPUT || mode == MODE_BIDIRECTIONAL;
    }

    private long getExtractableEnergy() {
        if (!canReadTarget()) {
            return 0L;
        }
        IEnergyStorage target = getTargetEnergyStorage();
        if (target == null || !target.canExtract()) {
            return 0L;
        }
        return Math.max(0, target.extractEnergy(Integer.MAX_VALUE, true));
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(IStorageChannel channel) {
        if (channel == FluxStorageChannel.INSTANCE) {
            return Collections.<IMEInventoryHandler>singletonList(handler);
        }
        return Collections.emptyList();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void blinkCell(int slot) {
    }

    @Override
    public void saveChanges(@Nullable ICellInventory<?> cellInventory) {
        saveChanges();
    }

    private class EnergyStorageHandler implements IMEInventoryHandler<FluxStack> {

        @Override
        public FluxStack injectItems(FluxStack input, Actionable action, appeng.api.networking.security.IActionSource source) {
            if (!canWriteTarget() || input == null || input.getStackSize() <= 0) {
                return input;
            }
            IEnergyStorage target = getTargetEnergyStorage();
            if (target == null || !target.canReceive()) {
                return input;
            }
            int amount = (int) Math.min(input.getStackSize(), Integer.MAX_VALUE);
            int received = target.receiveEnergy(amount, action == Actionable.SIMULATE);
            long remainder = input.getStackSize() - Math.max(0, received);
            return remainder > 0 ? new FluxStack(remainder) : null;
        }

        @Override
        public FluxStack extractItems(FluxStack request, Actionable action, appeng.api.networking.security.IActionSource source) {
            if (!canReadTarget() || request == null || request.getStackSize() <= 0) {
                return null;
            }
            IEnergyStorage target = getTargetEnergyStorage();
            if (target == null || !target.canExtract()) {
                return null;
            }
            int amount = (int) Math.min(request.getStackSize(), Integer.MAX_VALUE);
            int extracted = target.extractEnergy(amount, action == Actionable.SIMULATE);
            return extracted > 0 ? new FluxStack(extracted) : null;
        }

        @Override
        public IItemList<FluxStack> getAvailableItems(IItemList<FluxStack> out) {
            long available = getExtractableEnergy();
            if (available > 0) {
                out.add(new FluxStack(available));
            }
            return out;
        }

        @Override
        public IStorageChannel<FluxStack> getChannel() {
            return FluxStorageChannel.INSTANCE;
        }

        @Override
        public AccessRestriction getAccess() {
            switch (mode) {
                case MODE_INPUT:
                    return AccessRestriction.READ;
                case MODE_OUTPUT:
                    return AccessRestriction.WRITE;
                default:
                    return AccessRestriction.READ_WRITE;
            }
        }

        @Override
        public boolean isPrioritized(FluxStack input) {
            return false;
        }

        @Override
        public boolean canAccept(FluxStack input) {
            if (!canWriteTarget() || input == null || input.getStackSize() <= 0) {
                return false;
            }
            IEnergyStorage target = getTargetEnergyStorage();
            return target != null && target.canReceive();
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public boolean validForPass(int pass) {
            return true;
        }
    }
}
