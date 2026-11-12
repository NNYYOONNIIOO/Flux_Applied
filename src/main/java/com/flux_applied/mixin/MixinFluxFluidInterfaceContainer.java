package com.flux_applied.mixin;

import appeng.api.implementations.IUpgradeableHost;
import appeng.fluids.container.FluidConfigurableContainer;
import appeng.fluids.container.FluidInterfaceContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidInterfaceContainer.class, remap = false)
public abstract class MixinFluxFluidInterfaceContainer extends FluidConfigurableContainer {
    protected MixinFluxFluidInterfaceContainer(ContainerType<?> type, int id, PlayerInventory inventory, IUpgradeableHost host) {
        super(type, id, inventory, host);
    }

    @Inject(method = "availableUpgrades", at = @At("HEAD"), cancellable = true)
    private void flux_applied$oneSlot(CallbackInfoReturnable<Integer> callback) { callback.setReturnValue(1); }

    @Inject(method = "setupConfig", at = @At("TAIL"))
    private void flux_applied$setupSlot(CallbackInfo callback) { setupUpgrades(); }
}
