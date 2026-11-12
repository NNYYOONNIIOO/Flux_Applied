package com.flux_applied.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import net.minecraftforge.fml.InterModComms;

public class TOPHandler {

    public static void register() {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe",
                () -> (java.util.function.Function<ITheOneProbe, Void>) probe -> {
                    probe.registerProvider(new EnergyPortProbeProvider());
                    return null;
                });
    }
}
