package com.flux_applied.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import com.flux_applied.FluxApplied;

public class TOPHandler {

    public static void register() {
        FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", "com.flux_applied.top.TOPHandler$GetTheOneProbe");
        FluxApplied.getLogger().info("[FluxApplied] TOP integration registered");
    }

    public static class GetTheOneProbe implements java.util.function.Function<ITheOneProbe, Void> {

        @Override
        public Void apply(ITheOneProbe theOneProbe) {
            theOneProbe.registerProvider(new EnergyPortProbeProvider());
            FluxApplied.getLogger().info("[FluxApplied] TOP provider registered");
            return null;
        }
    }
}