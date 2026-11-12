package com.flux_applied.client.handler;

import appeng.container.slot.RestrictedInputSlot;
import com.flux_applied.FluxApplied;
import com.flux_applied.network.CPacketProviderCardCycle;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FluxApplied.MODID, value = Dist.CLIENT)
public final class ProviderCardClickHandler {
    private ProviderCardClickHandler() {}
    @SubscribeEvent
    public static void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getButton() != 1 || !(event.getGui() instanceof ContainerScreen<?>)) return;
        ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();
        Slot slot = screen.getSlotUnderMouse();
        if (!(slot instanceof RestrictedInputSlot)) return;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || stack.getItem() != FluxApplied.providerCard) return;
        FluxApplied.NETWORK.sendToServer(new CPacketProviderCardCycle(screen.getContainer().windowId, slot.slotNumber));
        event.setCanceled(true);
    }
}
