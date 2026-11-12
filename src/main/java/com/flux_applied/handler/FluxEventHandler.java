package com.flux_applied.handler;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import com.flux_applied.FluxApplied;
import com.flux_applied.capability.ProviderCardCapabilityProvider;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

public final class FluxEventHandler {
    private FluxEventHandler() {}
    @SubscribeEvent public static void attachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        TileEntity tile = event.getObject();
        if (!(tile instanceof IPartHost) && !ProviderCardHelper.isInterface(tile)) return;
        ProviderCardCapabilityProvider provider = new ProviderCardCapabilityProvider(tile);
        event.addCapability(FluxApplied.id("provider_card_energy"), provider);
        event.addListener(provider::invalidate);
    }
    @SubscribeEvent public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        if (!player.isSneaking()) return;
        ItemStack held = player.getHeldItem(event.getHand());
        boolean holdingCard = held.getItem() == FluxApplied.providerCard;
        if (!holdingCard && !held.isEmpty()) return;
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        Object host = tile;
        if (tile instanceof IPartHost) {
            Vector3d hit = event.getHitVec().getHitVec().subtract(Vector3d.copy(event.getPos()));
            SelectedPart selected = ((IPartHost) tile).selectPart(hit);
            host = selected.part;
        }
        if (!ProviderCardHelper.isInterface(host)) return;
        ItemStack card = ProviderCardHelper.findProviderCard(host);
        if (holdingCard ? !card.isEmpty() : card.isEmpty()) return;
        if (!event.getWorld().isRemote) {
            if (holdingCard) {
                IItemHandler upgrades = ProviderCardHelper.getUpgradeInventory(host);
                ItemStack single = held.copy();
                single.setCount(1);
                boolean installed = false;
                if (upgrades != null) {
                    for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                        if (upgrades.insertItem(slot, single, false).isEmpty()) {
                            if (!player.isCreative()) held.shrink(1);
                            installed = true;
                            break;
                        }
                    }
                }
                if (!installed) return;
            } else {
                ItemProviderCard.cycleMode(card);
                ItemProviderCard.sendModeMessage(player, ItemProviderCard.getMode(card));
            }
            ProviderCardHelper.upgradesChanged(host);
        }
        event.setCanceled(true);
        event.setCancellationResult(ActionResultType.SUCCESS);
    }
}
