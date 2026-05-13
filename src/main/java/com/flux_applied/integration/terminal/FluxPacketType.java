package com.flux_applied.integration.terminal;

import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.item.ItemFluxPacket;
import nyonio.terminal_interaction_integration.api.IPacketType;

public class FluxPacketType implements IPacketType {
    
    @Override
    public String getName() {
        return "flux";
    }
    
    @Override
    public String getDisplayName() {
        return "\u00a76FE";
    }
    
    @Override
    public boolean isPacket(ItemStack stack) {
        return ItemFluxPacket.isFluxPacket(stack);
    }
    
    @Override
    public long getAmount(ItemStack stack) {
        return ItemFluxPacket.getFE(stack);
    }
    
    @Override
    public IAEItemStack createAEStack(long amount) {
        return ItemFluxPacket.createAE(amount);
    }
    
    @Override
    public ItemStack createItemStack(long amount) {
        return ItemFluxPacket.create(amount);
    }
}
