package com.flux_applied.mixin;

import appeng.fluids.helper.DualityFluidInterface;
import appeng.fluids.helper.IFluidInterfaceHost;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.util.ProviderCardHelper;
import appeng.fluids.util.IAEFluidTank;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DualityFluidInterface.class, remap = false)
public abstract class MixinFluxFluidInterfaceUpgrades {
    @Shadow @Final private IFluidInterfaceHost iHost;

    @Inject(method = "onFluidInventoryChanged", at = @At("TAIL"))
    private void flux_applied$onInventoryChanged(IAEFluidTank inventory, int slot, CallbackInfo callback) {
        ProviderCardHelper.keepAwake(iHost);
    }

    @Inject(method = "getInventoryByName", at = @At("HEAD"), cancellable = true)
    private void flux_applied$upgrades(String name, CallbackInfoReturnable<IItemHandler> callback) {
        if ("upgrades".equals(name)) {
            InterfaceEnergyState state = ProviderCardHelper.state(iHost);
            if (state != null) callback.setReturnValue(state.fluidUpgrades());
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void flux_applied$write(CompoundNBT tag, CallbackInfo callback) {
        InterfaceEnergyState state = ProviderCardHelper.state(iHost);
        if (state != null) tag.put("FluxAppliedCard", state.fluidUpgrades().serializeNBT());
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void flux_applied$read(CompoundNBT tag, CallbackInfo callback) {
        InterfaceEnergyState state = ProviderCardHelper.state(iHost);
        if (state != null) {
            state.fluidUpgrades().deserializeNBT(tag.getCompound("FluxAppliedCard"));
            state.refreshCard();
        }
    }
}
