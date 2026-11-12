package com.flux_applied.mixin;

import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.tile.AEBaseTileEntity;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = AEBaseTileEntity.class, remap = false)
public abstract class MixinFluxFluidTileDrops {
    @Inject(method = "getDrops", at = @At("TAIL"))
    private void flux_applied$dropCard(World world, BlockPos pos, List<ItemStack> drops, CallbackInfo callback) {
        if (!((Object) this instanceof IFluidInterfaceHost)) return;
        InterfaceEnergyState state = ProviderCardHelper.state(this);
        if (state != null && state.hasCard()) drops.add(state.fluidUpgrades().getStackInSlot(0).copy());
    }
}
