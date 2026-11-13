package com.flux_applied.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.parts.misc.InterfacePart;
import com.flux_applied.util.ProviderCardHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = { InterfaceTileEntity.class, InterfacePart.class }, remap = false)
public abstract class MixinFluxInterfaceGridTickTargets {
    @Inject(method = "tickingRequest", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void flux_applied$onTickingRequest(IGridNode node, int ticks,
                                               CallbackInfoReturnable<TickRateModulation> callback) {
        com.flux_applied.energy.InterfaceEnergyState state = ProviderCardHelper.state(this);
        if (state == null || !state.hasCard() || node == null || !node.isActive()) return;
        boolean moved = com.flux_applied.handler.FluxInterfaceGridTickHandler.INSTANCE.tick(this);
        TickRateModulation previous = callback.getReturnValue();
        if (moved) callback.setReturnValue(TickRateModulation.URGENT);
        else if (previous == TickRateModulation.SLEEP) callback.setReturnValue(TickRateModulation.SLOWER);
    }
}
