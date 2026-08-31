package com.flux_applied.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPart;
import com.flux_applied.handler.FluxInterfaceGridTickHandler;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "appeng.tile.misc.TileInterface",
        "appeng.fluids.tile.TileFluidInterface",
        "appeng.parts.misc.PartInterface"
}, remap = false)
public abstract class MixinFluxInterfaceGridTickTargets {

    @Inject(method = "tickingRequest", at = @At("TAIL"), remap = false, require = 1)
    private void flux_applied$onTickingRequest(IGridNode node, int ticks,
                                                CallbackInfoReturnable<TickRateModulation> callback) {
        Object self = (Object) this;
        if (self instanceof TileEntity) {
            FluxInterfaceGridTickHandler.INSTANCE.onTileEntityTick((TileEntity) self);
        } else if (self instanceof IPart) {
            FluxInterfaceGridTickHandler.INSTANCE.onPartTick((IPart) self);
        }
    }
}
