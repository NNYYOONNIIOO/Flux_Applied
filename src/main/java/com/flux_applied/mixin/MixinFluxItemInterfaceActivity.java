package com.flux_applied.mixin;

import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import com.flux_applied.util.ProviderCardHelper;
import appeng.util.inv.InvOperation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinFluxItemInterfaceActivity {
    @Shadow @Final private IInterfaceHost iHost;
    @Inject(method = "onChangeInventory", at = @At("TAIL"))
    private void flux_applied$onUpgradeChanged(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added, CallbackInfo callback) {
        if (removed.getItem() == com.flux_applied.FluxApplied.providerCard
                || added.getItem() == com.flux_applied.FluxApplied.providerCard) {
            ProviderCardHelper.keepAwake(iHost);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void flux_applied$afterRead(net.minecraft.nbt.CompoundNBT tag, CallbackInfo callback) {
        ProviderCardHelper.keepAwake(iHost);
    }
}
