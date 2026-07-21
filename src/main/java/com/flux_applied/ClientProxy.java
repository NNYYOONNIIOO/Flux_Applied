package com.flux_applied;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.flux_applied.client.handler.FluxTerminalHandler;
import com.flux_applied.client.handler.ProviderCardClickHandler;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }
    
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new FluxTerminalHandler());
        MinecraftForge.EVENT_BUS.register(new ProviderCardClickHandler());
    }
    
    @Override
    public void registerModels() {
        if (ModConfig.ITEMS.enableFluxCellHousing && FluxApplied.fluxCellHousing != null) {
            registerItemModel(FluxApplied.fluxCellHousing, 0, "flux_cell_housing");
        }
        
        registerItemModel(FluxApplied.fluxStorageCell1k, 0, "flux_storage_cell_1k");
        registerItemModel(FluxApplied.fluxStorageCell4k, 0, "flux_storage_cell_4k");
        registerItemModel(FluxApplied.fluxStorageCell16k, 0, "flux_storage_cell_16k");
        registerItemModel(FluxApplied.fluxStorageCell64k, 0, "flux_storage_cell_64k");
        registerItemModel(FluxApplied.fluxStorageCell256k, 0, "flux_storage_cell_256k");
        registerItemModel(FluxApplied.fluxStorageCell1m, 0, "flux_storage_cell_1m");
        registerItemModel(FluxApplied.fluxStorageCell4m, 0, "flux_storage_cell_4m");
        registerItemModel(FluxApplied.fluxStorageCell16m, 0, "flux_storage_cell_16m");
        registerItemModel(FluxApplied.fluxStorageCell64m, 0, "flux_storage_cell_64m");
        registerItemModel(FluxApplied.fluxStorageCell256m, 0, "flux_storage_cell_256m");
        registerItemModel(FluxApplied.fluxStorageCell1g, 0, "flux_storage_cell_1g");

        if (ModConfig.ITEMS.enableFluxStorageComponent) {
            if (FluxApplied.fluxStorageComponent1k != null) registerItemModel(FluxApplied.fluxStorageComponent1k, 0, "flux_storage_component_1k");
            if (FluxApplied.fluxStorageComponent4k != null) registerItemModel(FluxApplied.fluxStorageComponent4k, 0, "flux_storage_component_4k");
            if (FluxApplied.fluxStorageComponent16k != null) registerItemModel(FluxApplied.fluxStorageComponent16k, 0, "flux_storage_component_16k");
            if (FluxApplied.fluxStorageComponent64k != null) registerItemModel(FluxApplied.fluxStorageComponent64k, 0, "flux_storage_component_64k");
            if (FluxApplied.fluxStorageComponent256k != null) registerItemModel(FluxApplied.fluxStorageComponent256k, 0, "flux_storage_component_256k");
            if (FluxApplied.fluxStorageComponent1m != null) registerItemModel(FluxApplied.fluxStorageComponent1m, 0, "flux_storage_component_1m");
            if (FluxApplied.fluxStorageComponent4m != null) registerItemModel(FluxApplied.fluxStorageComponent4m, 0, "flux_storage_component_4m");
            if (FluxApplied.fluxStorageComponent16m != null) registerItemModel(FluxApplied.fluxStorageComponent16m, 0, "flux_storage_component_16m");
            if (FluxApplied.fluxStorageComponent64m != null) registerItemModel(FluxApplied.fluxStorageComponent64m, 0, "flux_storage_component_64m");
            if (FluxApplied.fluxStorageComponent256m != null) registerItemModel(FluxApplied.fluxStorageComponent256m, 0, "flux_storage_component_256m");
            if (FluxApplied.fluxStorageComponent1g != null) registerItemModel(FluxApplied.fluxStorageComponent1g, 0, "flux_storage_component_1g");
        }

        registerItemModel(FluxApplied.energyPortItem, 0, "energy_port");
        registerItemModel(FluxApplied.fluxPacket, 0, "flux_packet");
        registerItemModel(FluxApplied.partEnergyProvider, 0, "part_energy_provider");
        registerItemModel(FluxApplied.energyStorageBus, 0, "energy_storage_bus");
        registerItemModel(FluxApplied.providerCard, 0, "energy_port_card");
    }
    
    private void registerItemModel(Item item, int meta, String name) {
        ModelLoader.setCustomModelResourceLocation(item, meta,
            new ModelResourceLocation(FluxApplied.MODID + ":" + name, "inventory"));
    }
}
