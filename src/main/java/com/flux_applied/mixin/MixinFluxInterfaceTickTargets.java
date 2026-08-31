package com.flux_applied.mixin;

import com.flux_applied.handler.FluxInterfaceTickHandler;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "appeng.tile.networking.TileCableBus",
        "appeng.tile.misc.TileInterface",
        "appeng.fluids.tile.TileFluidInterface"
}, remap = false)
public abstract class MixinFluxInterfaceTickTargets {

    @Inject(method = {"update", "func_73660_a"}, at = @At("TAIL"), remap = false, require = 0)
    private void flux_applied$onUpdate(CallbackInfo ci) {
        FluxInterfaceTickHandler.INSTANCE.onTileEntityTick((TileEntity) (Object) this);
    }
}
