package com.flux_applied.recipe;

import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public final class ConfigCondition implements ICondition {
    private static final ResourceLocation ID = FluxApplied.id("config");
    private final String option;
    private ConfigCondition(String option) { this.option = option; }
    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean test() {
        return "cell_housing".equals(option) ? ModConfig.CELL_HOUSING.get()
                : "storage_components".equals(option) && ModConfig.STORAGE_COMPONENTS.get();
    }

    public static final class Serializer implements IConditionSerializer<ConfigCondition> {
        @Override public void write(JsonObject json, ConfigCondition condition) { json.addProperty("option", condition.option); }
        @Override public ConfigCondition read(JsonObject json) { return new ConfigCondition(json.get("option").getAsString()); }
        @Override public ResourceLocation getID() { return ID; }
    }
}
