package com.flux_applied.tile;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.block.BlockEnergyPort;
import com.flux_applied.energy.ILongEnergyStorage;
import com.flux_applied.energy.NetworkEnergyStorage;
import com.flux_applied.util.EnergyTransferHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import javax.annotation.Nullable;
import java.util.EnumSet;
public final class TileEntityEnergyProvider extends TileEntity implements ITickableTileEntity, IGridProxyable, IActionHost, ILongEnergyStorage {
    private final AENetworkProxy proxy;
    private int mode = 2;
    private long maxTransfer;
    private boolean ready;
    private final NetworkEnergyStorage storage = new NetworkEnergyStorage(() -> this, () -> mode, () -> !isRemoved(), this::getMaxTransfer, () -> world == null ? 0 : world.getGameTime());
    private LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> storage);
    public TileEntityEnergyProvider() {
        super(FluxApplied.ENERGY_PORT_TILE);
        proxy = new AENetworkProxy(this, "proxy", new ItemStack(FluxApplied.energyPortItem), true);
        proxy.setValidSides(EnumSet.allOf(Direction.class));
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
    }
    @Override public void tick() {
        if (world == null || world.isRemote) return;
        if (!ready) { ready = true; proxy.onReady(); }
        if (mode == 0 || !proxy.isActive()) return;
        for (Direction side : Direction.values()) {
            net.minecraft.util.math.BlockPos adjacent = pos.offset(side);
            if (!world.isBlockLoaded(adjacent)) continue;
            TileEntity target = world.getTileEntity(adjacent);
            if (target == null) continue;
            IEnergyStorage destination = target.getCapability(CapabilityEnergy.ENERGY, side.getOpposite()).orElse(null);
            EnergyTransferHelper.push(storage, destination, getMaxTransfer());
        }
    }
    public void toggleMode(PlayerEntity player) {
        mode = (mode + 1) % 3;
        markDirty();
        if (world != null && getBlockState().hasProperty(BlockEnergyPort.MODE)) world.setBlockState(pos, getBlockState().with(BlockEnergyPort.MODE, mode), 3);
        player.sendStatusMessage(new TranslationTextComponent(mode == 0 ? "flux_applied.message.input_only" : mode == 1 ? "flux_applied.message.output_only" : "flux_applied.message.bidirectional"), true);
    }
    public int getMode() { return mode; }
    public boolean isNetworkConnected() { return proxy.isActive(); }
    public long getMaxTransfer() { return maxTransfer > 0 ? Math.min(maxTransfer, ModConfig.getEnergyPortTransferRate()) : ModConfig.getEnergyPortTransferRate(); }
    public void setMaxTransfer(long value) { maxTransfer = Math.max(1, value); markDirty(); }
    @Override public long getEnergyStoredLong() { return storage.getEnergyStoredLong(); }
    @Override public long getMaxEnergyStoredLong() { return Long.MAX_VALUE; }
    @Override public long receiveEnergyLong(long amount, boolean simulate) { return storage.receiveEnergyLong(amount, simulate); }
    @Override public long extractEnergyLong(long amount, boolean simulate) { return storage.extractEnergyLong(amount, simulate); }
    @Override public boolean canReceive() { return storage.canReceive(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) { return cap == CapabilityEnergy.ENERGY ? capability.cast() : super.getCapability(cap, side); }
    @Override public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);
        mode = tag.contains("Mode") ? Math.floorMod(tag.getInt("Mode"), 3) : 2;
        maxTransfer = tag.getLong("MaxTransfer");
        proxy.readFromNBT(tag);
    }
    @Override public CompoundNBT write(CompoundNBT tag) { super.write(tag); tag.putInt("Mode", mode); tag.putLong("MaxTransfer", maxTransfer); proxy.writeToNBT(tag); return tag; }
    @Override public void onLoad() { super.onLoad(); ready = false; }
    @Override public void remove() { proxy.remove(); ready = false; super.remove(); }
    @Override public void onChunkUnloaded() { proxy.onChunkUnloaded(); ready = false; super.onChunkUnloaded(); }
    @Override public void invalidateCaps() { super.invalidateCaps(); capability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); capability = LazyOptional.of(() -> storage); }
    @Override public AENetworkProxy getProxy() { return proxy; }
    @Override public DimensionalCoord getLocation() { return new DimensionalCoord(this); }
    @Override public IGridNode getGridNode(AEPartLocation direction) { return proxy.getNode(); }
    @Override public AECableType getCableConnectionType(AEPartLocation direction) { return AECableType.SMART; }
    @Override public void gridChanged() {}
    @Override public void saveChanges() { markDirty(); }
    @Override public IGridNode getActionableNode() { return proxy.getNode(); }
    @Override public void securityBreak() { if (world != null) world.destroyBlock(pos, true); }
}
