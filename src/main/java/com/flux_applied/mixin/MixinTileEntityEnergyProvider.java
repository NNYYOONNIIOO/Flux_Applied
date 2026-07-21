package com.flux_applied.mixin;

import com.flux_applied.tile.TileEntityEnergyProvider;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityEnergyProvider.class)
@Implements({
    @Interface(iface = ic2.api.energy.tile.IEnergySink.class, prefix = "ic2sink$"),
    @Interface(iface = ic2.api.energy.tile.IEnergySource.class, prefix = "ic2source$")
})
public abstract class MixinTileEntityEnergyProvider {

    @Shadow(remap = false)
    private boolean ic2Registered;

    @Inject(method = {"update", "func_73660_a"}, at = @At("HEAD"), remap = false)
    private void onUpdateHead(CallbackInfo ci) {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        if (self.isNetworkConnected() && !ic2Registered) {
            registerIC2();
        }
    }

    @Inject(method = {"invalidate", "func_145843_s"}, at = @At("HEAD"), remap = false)
    private void onInvalidate(CallbackInfo ci) {
        deregisterIC2();
    }

    @Inject(method = {"onChunkUnload", "func_76623_d"}, at = @At("HEAD"), remap = false)
    private void onChunkUnload(CallbackInfo ci) {
        deregisterIC2();
    }

    private void registerIC2() {
        if (ic2Registered) return;
        try {
            ic2Registered = true;
            MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileLoadEvent((ic2.api.energy.tile.IEnergyTile) (Object) this));
        } catch (Throwable e) {
            ic2Registered = false;
        }
    }

    private void deregisterIC2() {
        if (!ic2Registered) return;
        try {
            ic2Registered = false;
            MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileUnloadEvent((ic2.api.energy.tile.IEnergyTile) (Object) this));
        } catch (Throwable ignored) {}
    }

    // ===== IEnergySink (prefix: ic2sink$) =====

    public double ic2sink$getDemandedEnergy() {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        if (mode == 1 || !self.isNetworkConnected()) return 0;
        return Integer.MAX_VALUE * 0.25;
    }

    public double ic2sink$injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        if (mode == 1 || !self.isNetworkConnected()) return amount;
        long feToInject = (long) (amount * 4.0);
        int injected = self.receiveEnergy((int) Math.min(feToInject, Integer.MAX_VALUE), false);
        double leftoverEU = amount - (injected * 0.25);
        return Math.max(0, leftoverEU);
    }

    public int ic2sink$getSinkTier() {
        return 4;
    }

    public boolean ic2sink$acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, EnumFacing direction) {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        return mode == 0 || mode == 2;
    }

    // ===== IEnergySource (prefix: ic2source$) =====

    public double ic2source$getOfferedEnergy() {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        if (mode == 0 || !self.isNetworkConnected()) return 0;
        long availableFE = self.getEnergyStoredLong();
        return Math.min(availableFE * 0.25, Integer.MAX_VALUE * 0.25);
    }

    public void ic2source$drawEnergy(double amount) {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        if (mode == 0 || !self.isNetworkConnected()) return;
        long feToExtract = (long) (amount * 4.0);
        self.extractEnergy((int) Math.min(feToExtract, Integer.MAX_VALUE), false);
    }

    public int ic2source$getSourceTier() {
        return 4;
    }

    public boolean ic2source$emitsEnergyTo(ic2.api.energy.tile.IEnergyAcceptor receiver, EnumFacing direction) {
        TileEntityEnergyProvider self = (TileEntityEnergyProvider) (Object) this;
        int mode = self.getMode();
        return mode == 1 || mode == 2;
    }
}
