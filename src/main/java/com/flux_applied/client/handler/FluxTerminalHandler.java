package com.flux_applied.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.flux_applied.client.render.FluxRenderUtils;
import com.flux_applied.item.ItemFluxPacket;

@SideOnly(Side.CLIENT)
public class FluxTerminalHandler {

    @SubscribeEvent
    public void onRenderItem(RenderItemInFrameEvent event) {
        ItemStack stack = event.getItem();
        if (ItemFluxPacket.isFluxPacket(stack)) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            FluxRenderUtils.renderFluxPacketIntoGuiSlot(null, stack, font);
        }
    }
}