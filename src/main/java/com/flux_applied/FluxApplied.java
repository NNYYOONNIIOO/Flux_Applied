package com.flux_applied;

import appeng.core.Api;
import com.flux_applied.ae2.FluxCellHandler;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.block.BlockEnergyPort;
import com.flux_applied.handler.FluxEventHandler;
import com.flux_applied.integration.terminal.FluxResourceProvider;
import com.flux_applied.item.*;
import com.flux_applied.network.CPacketProviderCardCycle;
import com.flux_applied.part.*;
import com.flux_applied.tile.TileEntityEnergyProvider;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import nyonio.terminal_interaction_integration.api.ResourceRegistrationEvent;
import nyonio.terminal_interaction_integration.api.UpgradeModuleRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Mod(FluxApplied.MODID)
public final class FluxApplied {
    public static final String MODID = "flux_applied";
    public static final String VERSION = "1.2.6-1.16.5";
    private static final List<Item> ITEMS = new ArrayList<>();
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            id("main"), () -> "1", "1"::equals, "1"::equals);
    public static final BlockEnergyPort energyPortBlock = (BlockEnergyPort) new BlockEnergyPort().setRegistryName(id("energy_port"));
    public static final TileEntityType<TileEntityEnergyProvider> ENERGY_PORT_TILE =
            (TileEntityType<TileEntityEnergyProvider>) TileEntityType.Builder.create(
                    TileEntityEnergyProvider::new, energyPortBlock).build(null).setRegistryName(id("energy_port"));
    public static final Item energyPortItem = item("energy_port", new BlockItem(energyPortBlock, properties()));
    public static final Item fluxCellHousing = item("flux_cell_housing", new OptionalFluxItem(true));
    public static final ItemFluxPacket fluxPacket = item("flux_packet", new ItemFluxPacket());
    public static final ItemProviderCard providerCard = item("energy_port_card", new ItemProviderCard());
    public static final ItemPartEnergyProvider partEnergyProvider = item("part_energy_provider", new ItemPartEnergyProvider());
    public static final ItemPartEnergyStorageBus energyStorageBus = item("energy_storage_bus", new ItemPartEnergyStorageBus());
    public static final ItemPartEnergyImportBus energyImportBus = item("energy_import_bus", new ItemPartEnergyImportBus());
    public static final Item[] STORAGE_CELLS = createCells();
    public static final Item[] STORAGE_COMPONENTS = createComponents();

    public FluxApplied() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        bus.addGenericListener(Item.class, this::registerItems);
        bus.addGenericListener(Block.class, this::registerBlocks);
        bus.addGenericListener(TileEntityType.class, this::registerTiles);
        bus.addListener(this::setup);
        bus.addListener(this::registerTerminalResources);
        Api.instance().storage().registerStorageChannel(FluxStorageChannel.class, FluxStorageChannel.INSTANCE);
        Api.instance().registries().partModels().registerModels(Arrays.asList(
                id("part/energy_provider"), id("part/energy_provider_input"), id("part/energy_provider_output"),
                id("part/energy_storage_bus"), id("part/energy_import_bus"),
                id("part/energy_import_bus_off"), id("part/energy_import_bus_has_channel")));
        NETWORK.registerMessage(0, CPacketProviderCardCycle.class, CPacketProviderCardCycle::encode,
                CPacketProviderCardCycle::decode, CPacketProviderCardCycle::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        MinecraftForge.EVENT_BUS.register(FluxEventHandler.class);
    }

    private void registerItems(RegistryEvent.Register<Item> event) { ITEMS.forEach(event.getRegistry()::register); }
    private void registerBlocks(RegistryEvent.Register<Block> event) { event.getRegistry().register(energyPortBlock); }
    private void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) { event.getRegistry().register(ENERGY_PORT_TILE); }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraftforge.common.crafting.CraftingHelper.register(new com.flux_applied.recipe.ConfigCondition.Serializer());
            Api.instance().registries().cell().addCellHandler(FluxCellHandler.INSTANCE);
            UpgradeModuleRegistration.register(providerCard, 1);
        });
    }
    private void registerTerminalResources(ResourceRegistrationEvent event) { event.register(new FluxResourceProvider()); }
    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
    public static Item.Properties properties() { return new Item.Properties().group(FluxAppliedTab.INSTANCE); }
    private static <T extends Item> T item(String name, T item) {
        item.setRegistryName(id(name));
        ITEMS.add(item);
        return item;
    }
    private static Item[] createCells() {
        String[] names = { "1k", "4k", "16k", "64k", "256k", "1m", "4m", "16m", "64m", "256m", "1g" };
        Item[] result = new Item[names.length];
        for (int i = 0, size = 1; i < names.length; i++, size *= 4) result[i] = item("flux_storage_cell_" + names[i], new ItemFluxStorageCell(size));
        return result;
    }
    private static Item[] createComponents() {
        String[] names = { "1k", "4k", "16k", "64k", "256k", "1m", "4m", "16m", "64m", "256m", "1g" };
        Item[] result = new Item[names.length];
        int[] capacities = { 1000, 4000, 16000, 64000, 256000, 1000000, 4000000, 16000000, 64000000, 256000000, 1000000000 };
        for (int i = 0; i < names.length; i++) result[i] = item("flux_storage_component_" + names[i], new ItemFluxStorageComponent(capacities[i]));
        return result;
    }
}
