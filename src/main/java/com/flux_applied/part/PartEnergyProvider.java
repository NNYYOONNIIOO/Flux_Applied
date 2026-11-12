package com.flux_applied.part;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.energy.ILongEnergyStorage;
import com.flux_applied.energy.NetworkEnergyStorage;
import com.flux_applied.util.EnergyTransferHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import java.io.IOException;

public final class PartEnergyProvider extends FluxPart implements IGridTickable, ILongEnergyStorage {
    public static final IPartModel MODELS_BASE = new PartModel(FluxApplied.id("part/energy_provider"));
    public static final IPartModel MODELS_INPUT = new PartModel(FluxApplied.id("part/energy_provider_input"));
    public static final IPartModel MODELS_OUTPUT = new PartModel(FluxApplied.id("part/energy_provider_output"));
    private int mode = 2;
    private long maxTransfer;
    private final NetworkEnergyStorage storage = new NetworkEnergyStorage(
            () -> this, () -> mode, () -> getTile() != null && !getTile().isRemoved(),
            this::getMaxTransfer, () -> getTile() == null || getTile().getWorld() == null ? 0 : getTile().getWorld().getGameTime());
    private LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> storage);

    public PartEnergyProvider(ItemStack stack) { super(stack); }

    @Override public void getBoxes(IPartCollisionHelper boxes) {
        boxes.addBox(2, 2, 14, 14, 14, 16);
        boxes.addBox(5, 5, 12, 11, 11, 14);
    }
    @Override public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putInt("Mode", mode);
        tag.putLong("MaxTransfer", maxTransfer);
    }
    @Override public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        mode = tag.contains("Mode") ? Math.floorMod(tag.getInt("Mode"), 3) : 2;
        maxTransfer = Math.max(0, tag.getLong("MaxTransfer"));
    }
    @Override public void writeToStream(PacketBuffer buffer) throws IOException {
        super.writeToStream(buffer);
        buffer.writeByte(mode);
    }
    @Override public boolean readFromStream(PacketBuffer buffer) throws IOException {
        boolean changed = super.readFromStream(buffer);
        int old = mode;
        mode = Math.floorMod(buffer.readByte(), 3);
        return changed || old != mode;
    }
    @Override public IPartModel getStaticModels() {
        if (!isActive() || !isPowered()) return MODELS_BASE;
        return mode == 0 ? MODELS_INPUT : mode == 1 ? MODELS_OUTPUT : MODELS_BASE;
    }
    @Override public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d hit) {
        if (useFluxMemoryCard(player, hand)) return true;
        if (!player.getHeldItem(hand).isEmpty()) return false;
        if (!isRemote()) {
            mode = (mode + 1) % 3;
            saveChanges();
            getHost().markForUpdate();
            player.sendStatusMessage(new TranslationTextComponent(mode == 0 ? "flux_applied.message.input_only"
                    : mode == 1 ? "flux_applied.message.output_only" : "flux_applied.message.bidirectional"), true);
        }
        return true;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        return cap == CapabilityEnergy.ENERGY ? capability.cast() : LazyOptional.empty();
    }
    @Override public void removeFromWorld() {
        capability.invalidate();
        super.removeFromWorld();
    }
    @Override public void addToWorld() {
        capability = LazyOptional.of(() -> storage);
        super.addToWorld();
    }
    @Override public TickingRequest getTickingRequest(IGridNode node) { return new TickingRequest(1, 20, false, false); }
    @Override public TickRateModulation tickingRequest(IGridNode node, int ticks) {
        if (mode == 0 || !getProxy().isActive() || getTile() == null || getTile().getWorld() == null) return TickRateModulation.SLOWER;
        Direction facing = getSide().getFacing();
        BlockPos targetPos = getTile().getPos().offset(facing);
        if (!getTile().getWorld().isBlockLoaded(targetPos)) return TickRateModulation.SLOWER;
        TileEntity neighbor = getTile().getWorld().getTileEntity(targetPos);
        IEnergyStorage target = neighbor == null ? null : neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).orElse(null);
        return EnergyTransferHelper.push(storage, target, getMaxTransfer()) > 0 ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
    }
    public int getMode() { return mode; }
    public long getMaxTransfer() { return maxTransfer > 0 ? Math.min(maxTransfer, ModConfig.getEnergyPortTransferRate()) : ModConfig.getEnergyPortTransferRate(); }
    public long getNetworkEnergyStored() { return storage.getEnergyStoredLong(); }
    @Override public long receiveEnergyLong(long amount, boolean simulate) { return storage.receiveEnergyLong(amount, simulate); }
    @Override public long extractEnergyLong(long amount, boolean simulate) { return storage.extractEnergyLong(amount, simulate); }
    @Override public long getEnergyStoredLong() { return storage.getEnergyStoredLong(); }
    @Override public long getMaxEnergyStoredLong() { return Long.MAX_VALUE; }
    @Override public boolean canReceive() { return storage.canReceive(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
}
