package com.flux_applied.network;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.PlayerSource;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;

import java.io.IOException;

public class CPacketFluxContainerAction implements IMessage {

    private long feAmount;
    private boolean extractMode;

    public CPacketFluxContainerAction() {
    }

    public CPacketFluxContainerAction(long feAmount, boolean extractMode) {
        this.feAmount = feAmount;
        this.extractMode = extractMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.feAmount = buf.readLong();
        this.extractMode = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.feAmount);
        buf.writeBoolean(this.extractMode);
    }

    public long getFEAmount() {
        return this.feAmount;
    }

    public boolean isExtractMode() {
        return this.extractMode;
    }

    public static class Handler implements IMessageHandler<CPacketFluxContainerAction, IMessage> {

        @Override
        public IMessage onMessage(CPacketFluxContainerAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack heldItem = player.inventory.getItemStack();
                if (!heldItem.isEmpty() && heldItem.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
                    IEnergyStorage energyStorage = heldItem.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
                    
                    if (player.openContainer instanceof ContainerMEMonitorable) {
                        ContainerMEMonitorable container = (ContainerMEMonitorable) player.openContainer;
                        IStorageGrid grid = container.getNetworkNode().getGrid().getCache(IStorageGrid.class);
                        IActionSource source = new PlayerSource(player, (IActionHost) container.getTarget());
                        
                        if (grid != null) {
                            if (message.isExtractMode()) {
                                handleExtractFE(energyStorage, heldItem, grid, source, message.getFEAmount(), player);
                            } else {
                                handleInjectFE(energyStorage, heldItem, grid, source, player);
                            }
                        }
                    }
                }
            });
            return null;
        }
        
        private static void handleExtractFE(IEnergyStorage energyStorage, ItemStack heldItem, IStorageGrid grid, IActionSource source, long feAmount, EntityPlayerMP player) {
            int currentFE = energyStorage.getEnergyStored();
            int maxFE = energyStorage.getMaxEnergyStored();
            int space = maxFE - currentFE;
            
            if (space > 0) {
                int feToExtract = (int) Math.min(feAmount, space);
                FluxStack request = new FluxStack(feToExtract);
                FluxStack extracted = grid.getInventory(FluxStorageChannel.INSTANCE).extractItems(request, Actionable.MODULATE, source);
                
                if (extracted != null && extracted.getStackSize() > 0) {
                    energyStorage.receiveEnergy((int) extracted.getStackSize(), false);
                    updateHeld(player);
                }
            }
        }
        
        private static void handleInjectFE(IEnergyStorage energyStorage, ItemStack heldItem, IStorageGrid grid, IActionSource source, EntityPlayerMP player) {
            int currentFE = energyStorage.getEnergyStored();
            
            if (currentFE > 0) {
                FluxStack toInject = new FluxStack(currentFE);
                FluxStack leftover = grid.getInventory(FluxStorageChannel.INSTANCE).injectItems(toInject, Actionable.MODULATE, source);
                
                long injected = currentFE - (leftover != null ? leftover.getStackSize() : 0);
                
                if (injected > 0) {
                    energyStorage.extractEnergy((int) injected, false);
                    updateHeld(player);
                }
            }
        }
        
        private static void updateHeld(EntityPlayerMP player) {
            if (Platform.isServer()) {
                try {
                    NetworkHandler.instance().sendTo(
                        new PacketInventoryAction(
                            InventoryAction.UPDATE_HAND, 
                            0, 
                            AEItemStack.fromItemStack(player.inventory.getItemStack())
                        ), 
                        player
                    );
                } catch (IOException e) {
                    AELog.debug(e);
                }
            }
        }
    }
}