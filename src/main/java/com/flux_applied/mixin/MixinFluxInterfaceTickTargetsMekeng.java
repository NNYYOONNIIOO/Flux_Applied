package com.flux_applied.mixin;

import com.flux_applied.handler.FluxInterfaceTickHandler;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "com.glodblock.github.common.tile.TileTrioInterface",
        "com.mekeng.github.common.tile.TileGasInterface"
}, remap = false)
public abstract class MixinFluxInterfaceTickTargetsMekeng {

    @Inject(method = {"update", "func_73660_a"}, at = @At("TAIL"), remap = false, require = 0)
    private void flux_applied$onUpdate(CallbackInfo ci) {
        FluxInterfaceTickHandler.INSTANCE.onTileEntityTick((TileEntity) (Object) this);
    }
}
