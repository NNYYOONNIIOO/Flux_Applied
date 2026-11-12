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
import appeng.api.storage.cells.ICellContainer;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.parts.BasicStatePart;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;
import com.flux_applied.FluxApplied;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PartEnergyStorageBus extends FluxPart implements IGridTickable, ICellContainer {

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
    public void writeToStream(PacketBuffer data) throws IOException {
        super.writeToStream(data);
        data.writeByte((byte) mode);
    }

    @Override
    public boolean readFromStream(PacketBuffer data) throws IOException {
        boolean changed = super.readFromStream(data);
        int oldMode = mode;
        mode = data.readByte();
        if (mode < MODE_INPUT || mode > MODE_BIDIRECTIONAL) {
            mode = MODE_BIDIRECTIONAL;
        }
        return changed || oldMode != mode;
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putInt("Mode", mode);
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        mode = data.contains("Mode") ? data.getInt("Mode") : MODE_BIDIRECTIONAL;
        if (mode < MODE_INPUT || mode > MODE_BIDIRECTIONAL) {
            mode = MODE_BIDIRECTIONAL;
        }
    }

    @Override
    protected CompoundNBT downloadSettings(SettingsFrom from, CompoundNBT output) {
        super.downloadSettings(from, output);
        output.putInt("Mode", mode);
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, CompoundNBT compound, PlayerEntity player) {
        super.uploadSettings(from, compound, player);
        if (compound.contains("Mode")) {
            mode = compound.getInt("Mode");
            if (mode < MODE_INPUT || mode > MODE_BIDIRECTIONAL) {
                mode = MODE_BIDIRECTIONAL;
            }
            lastReportedEnergy = -1L;
            getHost().markForSave();
            getHost().markForUpdate();
            notifyCellArrayUpdate();
        }
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return MODELS;
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (useFluxMemoryCard(player, hand)) return true;
        if (!player.getHeldItem(hand).isEmpty()) {
            return false;
        }
        if (!getHost().getTile().getWorld().isRemote) {
            toggleMode(player);
        }
        return true;
    }

    public void toggleMode(PlayerEntity player) {
        mode = (mode + 1) % 3;
        lastReportedEnergy = -1L;
        saveChanges();
        getHost().markForUpdate();
        notifyCellArrayUpdate();
        player.sendStatusMessage(new TranslationTextComponent(getModeMessageKey()), true);
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

    public int getMode() {
        return mode;
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
        Direction facing = getSide().getFacing();
        if (hostTile.getWorld() == null || !hostTile.getWorld().isBlockLoaded(hostTile.getPos().offset(facing))) return null;
        TileEntity target = hostTile.getWorld().getTileEntity(hostTile.getPos().offset(facing));
        if (target == null) {
            return null;
        }
        return target.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).orElse(null);
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
        return target instanceof com.flux_applied.energy.ILongEnergyStorage
                ? Math.max(0, ((com.flux_applied.energy.ILongEnergyStorage) target).getEnergyStoredLong())
                : Math.max(0, target.getEnergyStored());
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
            long received = com.flux_applied.util.EnergyTransferHelper.receive(target, input.getStackSize(), action == Actionable.SIMULATE);
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
            long extracted = com.flux_applied.util.EnergyTransferHelper.extract(target, request.getStackSize(), action == Actionable.SIMULATE);
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
