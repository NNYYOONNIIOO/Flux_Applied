package com.flux_applied;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Loader;
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
import com.flux_applied.item.ItemFluxPacket;
import com.flux_applied.item.ItemFluxStorageCell;
import com.flux_applied.item.ItemFluxStorageComponent;
import com.flux_applied.FluxUpgrades;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.part.ItemPartEnergyProvider;
import com.flux_applied.part.PartEnergyProvider;
import com.flux_applied.part.ItemPartEnergyStorageBus;
import com.flux_applied.part.ItemPartEnergyImportBus;
import com.flux_applied.tile.TileEntityEnergyProvider;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.ae2.FluxCellHandler;
import com.flux_applied.network.CPacketFluxContainerAction;
import com.flux_applied.network.CPacketProviderCardCycle;
import com.flux_applied.handler.FluxEventHandler;
import com.flux_applied.handler.FluxInterfaceGridTickHandler;
import nyonio.terminal_interaction_integration.api.ResourceRegistrationEvent;
import nyonio.terminal_interaction_integration.api.UpgradeModuleRegistration;
import appeng.api.AEApi;
import net.minecraft.util.ResourceLocation;
import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.List;

@Mod(modid = FluxApplied.MODID, name = FluxApplied.NAME, version = FluxApplied.VERSION, dependencies = "required-after:appliedenergistics2;required-after:terminal_interaction_integration")
@Mod.EventBusSubscriber
public class FluxApplied implements ILateMixinLoader
{
    public static final String MODID = "flux_applied";
    public static final String NAME = "Flux Applied";
    public static final String VERSION = "1.2.4";

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

    public static ItemPartEnergyProvider partEnergyProvider;

    public static ItemPartEnergyStorageBus energyStorageBus;

    public static ItemPartEnergyImportBus energyImportBus;

    public static ItemProviderCard providerCard;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(CPacketFluxContainerAction.Handler.class, CPacketFluxContainerAction.class, 0, Side.SERVER);
        network.registerMessage(CPacketProviderCardCycle.Handler.class, CPacketProviderCardCycle.class, 1, Side.SERVER);

        AEApi.instance().storage().registerStorageChannel(FluxStorageChannel.class, FluxStorageChannel.INSTANCE);

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

        partEnergyProvider = new ItemPartEnergyProvider();

        energyStorageBus = new ItemPartEnergyStorageBus();

        energyImportBus = new ItemPartEnergyImportBus();

        providerCard = new ItemProviderCard();
        UpgradeModuleRegistration.register(providerCard, 1);

        if (Loader.isModLoaded("mekceuaeupgrade")) {
            MinecraftForge.EVENT_BUS.register(FluxInterfaceGridTickHandler.INSTANCE);
        }
        new FluxEventHandler();

        AEApi.instance().registries().partModels().registerModels(
            new ResourceLocation(MODID, "part/energy_provider"),
            new ResourceLocation(MODID, "part/energy_provider_input"),
            new ResourceLocation(MODID, "part/energy_provider_output"),
            new ResourceLocation(MODID, "part/energy_storage_bus"),
            new ResourceLocation(MODID, "part/energy_import_bus"),
            new ResourceLocation(MODID, "part/energy_import_bus_off"),
            new ResourceLocation(MODID, "part/energy_import_bus_has_channel")
        );
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        
        AEApi.instance().registries().cell().addCellHandler(FluxCellHandler.INSTANCE);

        if (net.minecraftforge.fml.common.Loader.isModLoaded("theoneprobe")) {
            com.flux_applied.top.TOPHandler.register();
        }

        GameRegistry.registerTileEntity(TileEntityEnergyProvider.class, "flux_applied:energy_port");

        // Register provider card upgrades
        // MixinUpgradeInventory redirects getType() to STICKY in updateUpgradeInfo,
        // so CRAFTING registration is NOT needed for IFluxUpgradeModule items.
        // MixinUpgradeInvFilter fully controls insertion logic via IFluxExtendedUpgradeInventory.
        // We register to FluxUpgrades for tooltip display only.
        AEApi.instance().definitions().parts().iface().maybeStack(1).ifPresent(is ->
            FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, is, 1));
        AEApi.instance().definitions().blocks().iface().maybeStack(1).ifPresent(is ->
            FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, is, 1));
        AEApi.instance().definitions().parts().fluidIface().maybeStack(1).ifPresent(is ->
            FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, is, 1));
        AEApi.instance().definitions().blocks().fluidIface().maybeStack(1).ifPresent(is ->
            FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, is, 1));

        // Register for ae2fcr interfaces if loaded
        if (Loader.isModLoaded("ae2fc")) {
            try {
                registerAE2FCRUpgrades();
            } catch (Exception ignored) {}
        }

        // Register for mekeng interfaces if loaded
        if (Loader.isModLoaded("mekeng")) {
            try {
                registerMekengUpgrades();
            } catch (Exception ignored) {}
        }
    }

    private void registerAE2FCRUpgrades() {
        // ae2fcr panel form Dual Interface
        try {
            Class<?> fcItemsClass = Class.forName("com.glodblock.github.loader.FCItems");
            java.lang.reflect.Field dualField = fcItemsClass.getField("PART_DUAL_INTERFACE");
            Object dualItem = dualField.get(null);
            if (dualItem instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) dualItem), 1);
            }
        } catch (Exception ignored) {}

        // ae2fcr panel form Trio Interface (requires mekeng)
        try {
            Class<?> fcGasItemsClass = Class.forName("com.glodblock.github.integration.mek.FCGasItems");
            java.lang.reflect.Field trioField = fcGasItemsClass.getField("PART_TRIO_INTERFACE");
            Object trioItem = trioField.get(null);
            if (trioItem instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) trioItem), 1);
            }
        } catch (Exception ignored) {}

        // ae2fcr block form Dual Interface
        try {
            Class<?> fcBlocksClass = Class.forName("com.glodblock.github.loader.FCBlocks");
            java.lang.reflect.Field dualBlockField = fcBlocksClass.getField("DUAL_INTERFACE");
            Object dualBlock = dualBlockField.get(null);
            if (dualBlock instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) dualBlock), 1);
            }
        } catch (Exception ignored) {}

        // ae2fcr block form Trio Interface (requires mekeng)
        try {
            Class<?> fcGasBlocksClass = Class.forName("com.glodblock.github.integration.mek.FCGasBlocks");
            java.lang.reflect.Field trioBlockField = fcGasBlocksClass.getField("TRIO_INTERFACE");
            Object trioBlock = trioBlockField.get(null);
            if (trioBlock instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) trioBlock), 1);
            }
        } catch (Exception ignored) {}
    }

    private void registerMekengUpgrades() {
        try {
            Class<?> itemAndBlocksClass = Class.forName("com.mekeng.github.common.ItemAndBlocks");
            // Panel form
            java.lang.reflect.Field gasField = itemAndBlocksClass.getField("GAS_INTERFACE_PART");
            Object gasItem = gasField.get(null);
            if (gasItem instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) gasItem), 1);
            }
            // Block form
            java.lang.reflect.Field gasBlockField = itemAndBlocksClass.getField("GAS_INTERFACE");
            Object gasBlock = gasBlockField.get(null);
            if (gasBlock instanceof Item) {
                FluxUpgrades.registerItem(FluxUpgrades.ENERGY_PORT_CARD, new ItemStack((Item) gasBlock), 1);
            }
        } catch (Exception ignored) {}
    }
    
    public static class TerminalIntegrationHandler {
        @SubscribeEvent
        public void onResourceRegistration(ResourceRegistrationEvent event) {
            event.register(new FluxResourceProvider());
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
        event.getRegistry().register(partEnergyProvider);
        event.getRegistry().register(energyStorageBus);
        event.getRegistry().register(energyImportBus);
        event.getRegistry().register(providerCard);
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

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new java.util.ArrayList<>();
        configs.add("flux_applied.mixins.json");
        if (Loader.isModLoaded("ic2")) {
            configs.add("flux_applied.mixins_ic2.json");
        }
        if (Loader.isModLoaded("ae2fc")) {
            configs.add("flux_applied.ae2fc.mixins.json");
        }
        if (Loader.isModLoaded("mekeng")) {
            configs.add("flux_applied.mekeng.mixins.json");
        }
        return configs;
    }
}
