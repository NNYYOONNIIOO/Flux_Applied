package com.flux_applied.mixin;

import appeng.api.networking.security.IActionHost;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.parts.misc.InterfacePart;
import com.flux_applied.energy.IFluxInterfaceHost;
import com.flux_applied.energy.InterfaceEnergyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = { InterfaceTileEntity.class, InterfacePart.class }, remap = false)
public abstract class MixinFluxInterfaceHost implements IFluxInterfaceHost {
    @Unique
    private InterfaceEnergyState flux_applied$state;

    @Override
    @Unique
    public InterfaceEnergyState fluxApplied$getState() {
        if (flux_applied$state == null) {
            flux_applied$state = new InterfaceEnergyState((IActionHost) (Object) this);
        }
        return flux_applied$state;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "getTickingRequest", at = @org.spongepowered.asm.mixin.injection.At("RETURN"), cancellable = true)
    private void flux_applied$tickingRequest(appeng.api.networking.IGridNode node,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<appeng.api.networking.ticking.TickingRequest> callback) {
        InterfaceEnergyState state = fluxApplied$getState();
        state.refreshCard();
        if (state.hasCard()) callback.setReturnValue(new appeng.api.networking.ticking.TickingRequest(1, 20, false, true));
    }
}
