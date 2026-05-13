package com.flux_applied;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import com.flux_applied.block.BlockEnergyPort;
import com.flux_applied.integration.terminal.FluxResourceProvider;
import com.flux_applied.item.ItemFluxStorageCell;
import com.flux_applied.item.ItemFluxStorageComponent;
import com.flux_applied.item.ItemFluxPacket;
import com.flux_applied.tile.TileEntityEnergyProvider;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.ae2.FluxCellHandler;
import com.flux_applied.network.CPacketFluxContainerAction;
import nyonio.terminal_interaction_integration.api.ResourceRegistrationEvent;
import appeng.api.AEApi;

@Mod(modid = FluxApplied.MODID, name = FluxApplied.NAME, version = FluxApplied.VERSION, dependencies = "required-after:appliedenergistics2;required-after:terminal_interaction_integration")
@Mod.EventBusSubscriber
public class FluxApplied
{
    public static final String MODID = "flux_applied";
    public static final String NAME = "Flux Applied";
    public static final String VERSION = "1.0.0";

    @SidedProxy(clientSide = "com.flux_applied.ClientProxy", serverSide = "com.flux_applied.CommonProxy")
    public static CommonProxy proxy;

    private static Logger logger;
    
    private static SimpleNetworkWrapper network;
    
    public static SimpleNetworkWrapper getNetwork() {
        return network;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Item fluxCellHousing;

    public static Item fluxStorageCell1k;
    public static Item fluxStorageCell4k;
    public static Item fluxStorageCell16k;
    public static Item fluxStorageCell64k;
    public static Item fluxStorageCell256k;
    public static Item fluxStorageCell1m;
    public static Item fluxStorageCell4m;
    public static Item fluxStorageCell16m;
    public static Item fluxStorageCell64m;
    public static Item fluxStorageCell256m;
    public static Item fluxStorageCell1g;

    public static Item fluxStorageComponent1k;
    public static Item fluxStorageComponent4k;
    public static Item fluxStorageComponent16k;
    public static Item fluxStorageComponent64k;
    public static Item fluxStorageComponent256k;
    public static Item fluxStorageComponent1m;
    public static Item fluxStorageComponent4m;
    public static Item fluxStorageComponent16m;
    public static Item fluxStorageComponent64m;
    public static Item fluxStorageComponent256m;
    public static Item fluxStorageComponent1g;

    public static Block energyPortBlock;
    public static Item energyPortItem;

    public static Item fluxPacket;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(CPacketFluxContainerAction.Handler.class, CPacketFluxContainerAction.class, 0, Side.SERVER);

        AEApi.instance().storage().registerStorageChannel(FluxStorageChannel.class, FluxStorageChannel.INSTANCE);
        logger.info("FluxStorageChannel registered");

        MinecraftForge.EVENT_BUS.register(new TerminalIntegrationHandler());

        if (ModConfig.ITEMS.enableFluxCellHousing) {
            fluxCellHousing = new Item().setRegistryName(MODID, "flux_cell_housing").setUnlocalizedName("flux_applied.flux_cell_housing").setCreativeTab(FluxAppliedTab.INSTANCE);
        }

        fluxStorageCell1k = new ItemFluxStorageCell(1).setRegistryName(MODID, "flux_storage_cell_1k").setUnlocalizedName("flux_applied.flux_storage_cell_1k").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell4k = new ItemFluxStorageCell(4).setRegistryName(MODID, "flux_storage_cell_4k").setUnlocalizedName("flux_applied.flux_storage_cell_4k").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell16k = new ItemFluxStorageCell(16).setRegistryName(MODID, "flux_storage_cell_16k").setUnlocalizedName("flux_applied.flux_storage_cell_16k").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell64k = new ItemFluxStorageCell(64).setRegistryName(MODID, "flux_storage_cell_64k").setUnlocalizedName("flux_applied.flux_storage_cell_64k").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell256k = new ItemFluxStorageCell(256).setRegistryName(MODID, "flux_storage_cell_256k").setUnlocalizedName("flux_applied.flux_storage_cell_256k").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell1m = new ItemFluxStorageCell(1024).setRegistryName(MODID, "flux_storage_cell_1m").setUnlocalizedName("flux_applied.flux_storage_cell_1m").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell4m = new ItemFluxStorageCell(4096).setRegistryName(MODID, "flux_storage_cell_4m").setUnlocalizedName("flux_applied.flux_storage_cell_4m").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell16m = new ItemFluxStorageCell(16384).setRegistryName(MODID, "flux_storage_cell_16m").setUnlocalizedName("flux_applied.flux_storage_cell_16m").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell64m = new ItemFluxStorageCell(65536).setRegistryName(MODID, "flux_storage_cell_64m").setUnlocalizedName("flux_applied.flux_storage_cell_64m").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell256m = new ItemFluxStorageCell(262144).setRegistryName(MODID, "flux_storage_cell_256m").setUnlocalizedName("flux_applied.flux_storage_cell_256m").setCreativeTab(FluxAppliedTab.INSTANCE);
        fluxStorageCell1g = new ItemFluxStorageCell(1048576).setRegistryName(MODID, "flux_storage_cell_1g").setUnlocalizedName("flux_applied.flux_storage_cell_1g").setCreativeTab(FluxAppliedTab.INSTANCE);

        if (ModConfig.ITEMS.enableFluxStorageComponent) {
            fluxStorageComponent1k = new ItemFluxStorageComponent(1000).setRegistryName(MODID, "flux_storage_component_1k").setUnlocalizedName("flux_applied.flux_storage_component_1k").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent4k = new ItemFluxStorageComponent(4000).setRegistryName(MODID, "flux_storage_component_4k").setUnlocalizedName("flux_applied.flux_storage_component_4k").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent16k = new ItemFluxStorageComponent(16000).setRegistryName(MODID, "flux_storage_component_16k").setUnlocalizedName("flux_applied.flux_storage_component_16k").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent64k = new ItemFluxStorageComponent(64000).setRegistryName(MODID, "flux_storage_component_64k").setUnlocalizedName("flux_applied.flux_storage_component_64k").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent256k = new ItemFluxStorageComponent(256000).setRegistryName(MODID, "flux_storage_component_256k").setUnlocalizedName("flux_applied.flux_storage_component_256k").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent1m = new ItemFluxStorageComponent(1000000).setRegistryName(MODID, "flux_storage_component_1m").setUnlocalizedName("flux_applied.flux_storage_component_1m").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent4m = new ItemFluxStorageComponent(4000000).setRegistryName(MODID, "flux_storage_component_4m").setUnlocalizedName("flux_applied.flux_storage_component_4m").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent16m = new ItemFluxStorageComponent(16000000).setRegistryName(MODID, "flux_storage_component_16m").setUnlocalizedName("flux_applied.flux_storage_component_16m").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent64m = new ItemFluxStorageComponent(64000000).setRegistryName(MODID, "flux_storage_component_64m").setUnlocalizedName("flux_applied.flux_storage_component_64m").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent256m = new ItemFluxStorageComponent(256000000).setRegistryName(MODID, "flux_storage_component_256m").setUnlocalizedName("flux_applied.flux_storage_component_256m").setCreativeTab(FluxAppliedTab.INSTANCE);
            fluxStorageComponent1g = new ItemFluxStorageComponent(1000000000).setRegistryName(MODID, "flux_storage_component_1g").setUnlocalizedName("flux_applied.flux_storage_component_1g").setCreativeTab(FluxAppliedTab.INSTANCE);
        }

        energyPortBlock = new BlockEnergyPort();
        energyPortItem = new ItemBlock(energyPortBlock).setRegistryName(energyPortBlock.getRegistryName()).setCreativeTab(FluxAppliedTab.INSTANCE);

        fluxPacket = new ItemFluxPacket();

        logger.info("Flux Applied mod pre-initialized!");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        
        AEApi.instance().registries().cell().addCellHandler(FluxCellHandler.INSTANCE);
        logger.info("FluxCellHandler registered");

        if (net.minecraftforge.fml.common.Loader.isModLoaded("theoneprobe")) {
            com.flux_applied.top.TOPHandler.register();
        }

        GameRegistry.registerTileEntity(TileEntityEnergyProvider.class, "flux_applied:energy_port");
        
        logger.info("Flux Applied mod initialized!");
    }
    
    public static class TerminalIntegrationHandler {
        @SubscribeEvent
        public void onResourceRegistration(ResourceRegistrationEvent event) {
            event.register(new FluxResourceProvider());
            logger.info("[FluxApplied] FluxResourceProvider registered with Terminal Interaction Integration");
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        if (ModConfig.ITEMS.enableFluxCellHousing && fluxCellHousing != null) {
            event.getRegistry().register(fluxCellHousing);
        }
        event.getRegistry().register(fluxStorageCell1k);
        event.getRegistry().register(fluxStorageCell4k);
        event.getRegistry().register(fluxStorageCell16k);
        event.getRegistry().register(fluxStorageCell64k);
        event.getRegistry().register(fluxStorageCell256k);
        event.getRegistry().register(fluxStorageCell1m);
        event.getRegistry().register(fluxStorageCell4m);
        event.getRegistry().register(fluxStorageCell16m);
        event.getRegistry().register(fluxStorageCell64m);
        event.getRegistry().register(fluxStorageCell256m);
        event.getRegistry().register(fluxStorageCell1g);
        if (ModConfig.ITEMS.enableFluxStorageComponent) {
            if (fluxStorageComponent1k != null) event.getRegistry().register(fluxStorageComponent1k);
            if (fluxStorageComponent4k != null) event.getRegistry().register(fluxStorageComponent4k);
            if (fluxStorageComponent16k != null) event.getRegistry().register(fluxStorageComponent16k);
            if (fluxStorageComponent64k != null) event.getRegistry().register(fluxStorageComponent64k);
            if (fluxStorageComponent256k != null) event.getRegistry().register(fluxStorageComponent256k);
            if (fluxStorageComponent1m != null) event.getRegistry().register(fluxStorageComponent1m);
            if (fluxStorageComponent4m != null) event.getRegistry().register(fluxStorageComponent4m);
            if (fluxStorageComponent16m != null) event.getRegistry().register(fluxStorageComponent16m);
            if (fluxStorageComponent64m != null) event.getRegistry().register(fluxStorageComponent64m);
            if (fluxStorageComponent256m != null) event.getRegistry().register(fluxStorageComponent256m);
            if (fluxStorageComponent1g != null) event.getRegistry().register(fluxStorageComponent1g);
        }
        event.getRegistry().register(fluxPacket);
        event.getRegistry().register(energyPortItem);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().register(energyPortBlock);
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event)
    {
        proxy.registerModels();
    }
}
