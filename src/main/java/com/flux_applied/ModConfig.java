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
    
    @Config.Name("Energy")
    @Config.Comment("Energy transfer settings")
    public static final Energy ENERGY = new Energy();

    @Config.Name("Energy Import Bus")
    @Config.Comment("Energy Import Bus transfer settings")
    public static final EnergyImportBus ENERGY_IMPORT_BUS = new EnergyImportBus();

    @Config.Name("Integration")
    @Config.Comment("Compatibility integrations")
    public static final Integration INTEGRATION = new Integration();

    public static class Energy {
        @Config.Name("Energy Port Transfer Rate")
        @Config.Comment("Maximum FE transferred by an Energy Port Card per tick")
        public String energyPortTransferRate = String.valueOf(Integer.MAX_VALUE);
    }

    public static class EnergyImportBus {
        @Config.Name("Maximum Input Extract Rate")
        @Config.Comment("Maximum FE extracted by an Energy Import Bus per tick; supports values up to Long.MAX_VALUE")
        public String maximumInputExtractRate = String.valueOf(Integer.MAX_VALUE);

        @Config.Name("Enable Rate Growth")
        @Config.Comment("Allow the active Energy Import Bus transfer rate to grow over time")
        public boolean enableRateGrowth = true;

        @Config.Name("Rate Growth Per Tick")
        @Config.Comment("FE/t added to an active Energy Import Bus transfer rate each tick")
        public String rateGrowthPerTick = "40";

        @Config.Name("Initial Input Extract Rate")
        @Config.Comment("Initial FE/t for a newly placed Energy Import Bus")
        public String initialInputExtractRate = "40";
    }

    public static class Integration {
        @Config.Name("Enable Mekanism CEU AE Upgrade")
        @Config.Comment("Allow Mekanism CEU AE Upgrade machines to pull FE from AE networks")
        public boolean enableMekanismCeuAeUpgrade = true;

        @Config.Name("Mekanism CEU AE Upgrade Extract Rate")
        @Config.Comment("Maximum FE extracted into a Mekanism machine per tick")
        public String mekanismCeuAeUpgradeExtractRate = "128000";
    }

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
    
    public static long getEnergyPortTransferRate() {
        return parsePositiveLong(ENERGY.energyPortTransferRate, Integer.MAX_VALUE);
    }

    public static long getMekanismCeuAeUpgradeExtractRate() {
        return parsePositiveLong(INTEGRATION.mekanismCeuAeUpgradeExtractRate, 128000L);
    }

    public static long getEnergyImportBusMaximumInputExtractRate() {
        return parsePositiveLong(ENERGY_IMPORT_BUS.maximumInputExtractRate, Integer.MAX_VALUE);
    }

    public static boolean isEnergyImportBusRateGrowthEnabled() {
        return ENERGY_IMPORT_BUS.enableRateGrowth;
    }

    public static long getEnergyImportBusRateGrowthPerTick() {
        return parsePositiveLong(ENERGY_IMPORT_BUS.rateGrowthPerTick, 40L);
    }

    public static long getEnergyImportBusInitialInputExtractRate() {
        long maximum = getEnergyImportBusMaximumInputExtractRate();
        return Math.min(parsePositiveLong(ENERGY_IMPORT_BUS.initialInputExtractRate, 40L), maximum);
    }

    private static long parsePositiveLong(String value, long fallback) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
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
