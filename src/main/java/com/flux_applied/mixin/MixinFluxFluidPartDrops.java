package com.flux_applied.mixin;

import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.parts.AEBasePart;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = AEBasePart.class, remap = false)
public abstract class MixinFluxFluidPartDrops {
    @Inject(method = "getDrops", at = @At("TAIL"))
    private void flux_applied$dropCard(List<ItemStack> drops, boolean wrenched, CallbackInfo callback) {
        if (!((Object) this instanceof IFluidInterfaceHost)) return;
        InterfaceEnergyState state = ProviderCardHelper.state(this);
        if (state != null && state.hasCard()) drops.add(state.fluidUpgrades().getStackInSlot(0).copy());
    }
}
