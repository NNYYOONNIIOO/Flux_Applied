package com.flux_applied.client.handler;

import appeng.container.slot.SlotRestrictedInput;
import com.flux_applied.FluxApplied;
import com.flux_applied.network.CPacketProviderCardCycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class ProviderCardClickHandler {

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (Mouse.getEventButton() != 1 || !Mouse.getEventButtonState()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainer)) return;

        GuiContainer gui = (GuiContainer) mc.currentScreen;

        Slot slot = getSlotAtMouse(gui, Mouse.getX(), Mouse.getY());
        if (slot == null) return;
        if (!(slot instanceof SlotRestrictedInput)) return;

        SlotRestrictedInput restrictedSlot = (SlotRestrictedInput) slot;
        if (restrictedSlot.getPlaceableItemType() != SlotRestrictedInput.PlacableItemType.UPGRADES) return;

        ItemStack stackInSlot = slot.getStack();
        if (stackInSlot.isEmpty() || stackInSlot.getItem() != FluxApplied.providerCard) return;

        event.setCanceled(true);

        int slotIndex = slot.slotNumber;
        FluxApplied.getNetwork().sendToServer(new CPacketProviderCardCycle(slotIndex));
    }

    private Slot getSlotAtMouse(GuiContainer gui, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;
        int x = mouseX * gui.width / displayWidth;
        int y = gui.height - mouseY * gui.height / displayHeight - 1;

        int guiLeft = gui.getGuiLeft();
        int guiTop = gui.getGuiTop();

        for (Slot slot : gui.inventorySlots.inventorySlots) {
            int slotX = guiLeft + slot.xPos;
            int slotY = guiTop + slot.yPos;
            if (x >= slotX && x < slotX + 16 && y >= slotY && y < slotY + 16) {
                return slot;
            }
        }
        return null;
    }
}
