package com.flux_applied;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CELL_HOUSING;
    public static final ForgeConfigSpec.BooleanValue STORAGE_COMPONENTS;
    private static final ForgeConfigSpec.LongValue PORT_RATE, IMPORT_MAX, IMPORT_INITIAL, IMPORT_GROWTH;
    private static final ForgeConfigSpec.BooleanValue GROWTH_ENABLED;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("items");
        CELL_HOUSING = b.comment("Show the Flux Cell Housing item and enable its recipes.").define("enableFluxCellHousing", false);
        STORAGE_COMPONENTS = b.comment("Show Flux Storage Components and enable their recipes.").define("enableFluxStorageComponents", true);
        b.pop().push("energy");
        PORT_RATE = b.comment("Maximum FE transferred per port or interface card each tick. Supports long values.").defineInRange("energyPortTransferRate", Long.MAX_VALUE, 1L, Long.MAX_VALUE);
        b.pop().push("energyImportBus");
        IMPORT_MAX = b.defineInRange("maximumInputExtractRate", Long.MAX_VALUE, 1L, Long.MAX_VALUE);
        IMPORT_INITIAL = b.defineInRange("initialInputExtractRate", 40L, 1L, Long.MAX_VALUE);
        GROWTH_ENABLED = b.define("enableRateGrowth", true);
        IMPORT_GROWTH = b.defineInRange("rateGrowthPerTick", 40L, 0L, Long.MAX_VALUE);
        b.pop();
        SPEC = b.build();
    }
    private ModConfig() {}
    public static long getEnergyPortTransferRate() { return PORT_RATE.get(); }
    public static long getEnergyImportBusMaximumInputExtractRate() { return IMPORT_MAX.get(); }
    public static long getEnergyImportBusInitialInputExtractRate() { return Math.min(IMPORT_INITIAL.get(), IMPORT_MAX.get()); }
    public static boolean isEnergyImportBusRateGrowthEnabled() { return GROWTH_ENABLED.get(); }
    public static long getEnergyImportBusRateGrowthPerTick() { return IMPORT_GROWTH.get(); }
}
