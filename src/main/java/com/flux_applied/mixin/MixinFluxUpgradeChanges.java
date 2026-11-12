package com.flux_applied.mixin;

import appeng.helpers.DualityInterface;
import appeng.parts.automation.UpgradeInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.flux_applied.FluxApplied;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpgradeInventory.class, remap = false)
public abstract class MixinFluxUpgradeChanges {
    @Shadow @Final private IAEAppEngInventory parent;

    @Inject(method = "onChangeInventory", at = @At("TAIL"))
    private void flux_applied$cardChanged(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added, CallbackInfo callback) {
        if (!(parent instanceof DualityInterface)) return;
        if (removed.getItem() != FluxApplied.providerCard && added.getItem() != FluxApplied.providerCard) return;
        ProviderCardHelper.upgradesChanged(((DualityInterface) parent).getHost());
    }
}
