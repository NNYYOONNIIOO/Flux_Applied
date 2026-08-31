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
        "com.glodblock.github.common.tile.TileTrioInterface",
        "com.glodblock.github.common.part.PartTrioInterface",
        "com.mekeng.github.common.tile.TileGasInterface",
        "com.mekeng.github.common.part.PartGasInterface"
}, remap = false)
public abstract class MixinFluxInterfaceGridTickTargetsMekeng {

    @Inject(method = "tickingRequest", at = @At("TAIL"), remap = false, require = 0)
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
