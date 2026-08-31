package com.flux_applied.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPart;
import appeng.util.inv.InvOperation;
import com.flux_applied.handler.FluxInterfaceGridTickHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "com.glodblock.github.common.tile.TileDualInterface",
        "com.glodblock.github.common.part.PartDualInterface"
}, remap = false)
public abstract class MixinFluxInterfaceGridTickTargetsAE2FC {

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

    @Inject(method = "onChangeInventory", at = @At("TAIL"), remap = false, require = 0)
    private void flux_applied$onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
                                                 ItemStack removed, ItemStack added, CallbackInfo callback) {
        FluxInterfaceGridTickHandler.INSTANCE.invalidateCardCache(this);
    }
}
