package com.flux_applied.handler;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.fluids.tile.TileFluidInterface;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import com.flux_applied.FluxApplied;
import com.flux_applied.capability.ProviderCardCapabilityProvider;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

/**
 * Handles capability attachment and sneak-right-click interaction for Provider Cards.
 */
public class FluxEventHandler {

    private static final ResourceLocation CAPABILITY_ID = new ResourceLocation(FluxApplied.MODID, "provider_card_energy");

    public FluxEventHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ======================== Capability Attachment ========================

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        TileEntity te = event.getObject();
        if (te instanceof TileCableBus || te instanceof TileInterface || te instanceof TileFluidInterface) {
            event.addCapability(CAPABILITY_ID, new ProviderCardCapabilityProvider(te));
            return;
        }
        // ae2fcr block Dual/Trio Interface and mekeng block Gas Interface
        String className = te.getClass().getName();
        if (className.contains("TileDualInterface") || className.contains("TileTrioInterface") || className.contains("TileGasInterface")) {
            event.addCapability(CAPABILITY_ID, new ProviderCardCapabilityProvider(te));
        }
    }

    // ======================== Sneak-Right-Click Interaction ========================

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking()) return;

        ItemStack heldItem = player.getHeldItem(event.getHand());
        boolean holdingCard = !heldItem.isEmpty() && heldItem.getItem() == FluxApplied.providerCard;
        boolean emptyHand = heldItem.isEmpty();

        if (!holdingCard && !emptyHand) return;

        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (te == null) return;

        // Panel form: TileCableBus
        if (te instanceof TileCableBus) {
            // Use selectPart with hit vector to find the actual part being clicked
            // This works for both facing-based parts and INTERNAL parts (e.g. ae2fcr dual/trio interfaces)
            Vec3d hitVec = new Vec3d(
                    event.getHitVec().x - event.getPos().getX(),
                    event.getHitVec().y - event.getPos().getY(),
                    event.getHitVec().z - event.getPos().getZ()
            );
            SelectedPart selected = ((IPartHost) te).selectPart(hitVec);
            IPart part = selected.part;
            if (part == null || !ProviderCardHelper.isInterfacePart(part)) return;

            if (holdingCard) {
                if (ProviderCardHelper.findProviderCard(part) != null) return;

                if (!event.getWorld().isRemote) {
                    installCard(part, heldItem);
                }
                event.setUseBlock(PlayerInteractEvent.Result.DENY);
                event.setUseItem(PlayerInteractEvent.Result.DENY);
                return;
            }

            if (emptyHand) {
                ItemStack card = ProviderCardHelper.findProviderCard(part);
                if (card == null) return;

                if (!event.getWorld().isRemote) {
                    ItemProviderCard.cycleMode(card);
                    ItemProviderCard.sendModeMessage(player, ItemProviderCard.getMode(card));
                }
                event.setUseBlock(PlayerInteractEvent.Result.DENY);
                event.setUseItem(PlayerInteractEvent.Result.DENY);
                return;
            }
        }

        // Block form: TileInterface / TileFluidInterface / ae2fcr TileDualInterface,TileTrioInterface / mekeng TileGasInterface
        if (isBlockFormInterface(te)) {
            if (te instanceof IPartHost) return;

            if (holdingCard) {
                ItemStack existingCard = ProviderCardHelper.findProviderCardInBlockTE(te);
                if (existingCard != null) return;

                if (!event.getWorld().isRemote) {
                    installCardInBlockTE(te, heldItem);
                }
                event.setUseBlock(PlayerInteractEvent.Result.DENY);
                event.setUseItem(PlayerInteractEvent.Result.DENY);
                return;
            }

            if (emptyHand) {
                ItemStack card = ProviderCardHelper.findProviderCardInBlockTE(te);
                if (card == null) return;

                if (!event.getWorld().isRemote) {
                    ItemProviderCard.cycleMode(card);
                    ItemProviderCard.sendModeMessage(player, ItemProviderCard.getMode(card));
                }
                event.setUseBlock(PlayerInteractEvent.Result.DENY);
                event.setUseItem(PlayerInteractEvent.Result.DENY);
                return;
            }
        }
    }

    // ======================== Helper Methods ========================

    private boolean isBlockFormInterface(TileEntity te) {
        if (te instanceof TileInterface || te instanceof TileFluidInterface) return true;
        String className = te.getClass().getName();
        return className.contains("TileDualInterface") || className.contains("TileTrioInterface") || className.contains("TileGasInterface");
    }

    private void installCard(IPart part, ItemStack heldItem) {
        IItemHandler upgrades = ProviderCardHelper.getUpgradeInventory(part);
        if (upgrades == null) return;

        ItemStack toInsert = heldItem.copy();
        toInsert.setCount(1);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).isEmpty()) {
                ItemStack remainder = upgrades.insertItem(i, toInsert, false);
                if (remainder.isEmpty()) {
                    heldItem.shrink(1);
                    return;
                }
            }
        }
    }

    private void installCardInBlockTE(TileEntity te, ItemStack heldItem) {
        IItemHandler upgrades = ProviderCardHelper.getUpgradeInventoryFromBlockTE(te);
        if (upgrades == null) return;

        ItemStack toInsert = heldItem.copy();
        toInsert.setCount(1);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).isEmpty()) {
                ItemStack remainder = upgrades.insertItem(i, toInsert, false);
                if (remainder.isEmpty()) {
                    heldItem.shrink(1);
                    return;
                }
            }
        }
    }

}
