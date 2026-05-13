package com.flux_applied.integration.terminal;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.flux_applied.ae2.FluxStorageChannel;
import nyonio.terminal_interaction_integration.api.IContainerHandler;
import nyonio.terminal_interaction_integration.api.IPacketType;
import nyonio.terminal_interaction_integration.api.IResourceProvider;

public class FluxResourceProvider implements IResourceProvider {
    
    private final FluxPacketType packetType;
    private final FluxContainerHandler containerHandler;
    
    public FluxResourceProvider() {
        this.packetType = new FluxPacketType();
        this.containerHandler = new FluxContainerHandler();
    }
    
    @Override
    public String getName() {
        return "flux";
    }
    
    @Override
    public IStorageChannel<? extends IAEStack<?>> getStorageChannel() {
        return FluxStorageChannel.INSTANCE;
    }
    
    @Override
    public IPacketType getPacketType() {
        return packetType;
    }
    
    @Override
    public IContainerHandler getContainerHandler() {
        return containerHandler;
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}
