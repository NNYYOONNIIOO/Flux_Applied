package com.flux_applied;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = FluxApplied.MODID)
public class ModConfig {
    
    @Config.Name("Items")
    @Config.Comment("Item settings")
    public static final Items ITEMS = new Items();
    
    public static class Items {
        @Config.Name("Enable Flux Cell Housing")
        @Config.Comment("Set to true to enable the Flux Cell Housing item")
        @Config.RequiresWorldRestart
        public boolean enableFluxCellHousing = false;
        
        @Config.Name("Enable Flux Storage Components")
        @Config.Comment("Set to true to enable the ME Flux Storage Component items")
        @Config.RequiresWorldRestart
        public boolean enableFluxStorageComponent = true;
    }
    
    @Mod.EventBusSubscriber(modid = FluxApplied.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(FluxApplied.MODID)) {
                ConfigManager.sync(FluxApplied.MODID, Config.Type.INSTANCE);
            }
        }
    }
}