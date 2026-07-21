package com.flux_applied.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.flux_applied.item.ItemFluxPacket;

public class FluxRenderUtils {

    private static final ResourceLocation FE_TEXTURE = new ResourceLocation("flux_applied:items/fe_packet");

    public static void renderFEIntoGui(int x, int y, int width, int height, long fe) {
        if (fe <= 0) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(FE_TEXTURE.toString());

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        int x2 = x + width;

        buf.begin(7, DefaultVertexFormats.POSITION_TEX);
        double u1 = sprite.getMinU();
        double v1 = sprite.getMinV();
        double u2 = sprite.getMaxU();
        double v2 = sprite.getMaxV();

        buf.pos(x, y, 0).tex(u1, v1).endVertex();
        buf.pos(x, y + height, 0).tex(u1, v2).endVertex();
        buf.pos(x2, y + height, 0).tex(u2, v2).endVertex();
        buf.pos(x2, y, 0).tex(u2, v1).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    public static boolean renderFluxPacketIntoGuiSlot(Slot slot, ItemStack stack,
                                                       FontRenderer fontRenderer) {
        if (!ItemFluxPacket.isFluxPacket(stack)) {
            return false;
        }

        long fe = ItemFluxPacket.getFE(stack);
        if (fe <= 0) {
            return false;
        }

        renderFEIntoGui(slot.xPos, slot.yPos, 16, 16, fe);

        String displayText;
        if (fe >= 1000000000) {
            displayText = String.format("%.1fG", fe / 1000000000.0);
        } else if (fe >= 1000000) {
            displayText = String.format("%.1fM", fe / 1000000.0);
        } else if (fe >= 1000) {
            displayText = String.format("%.1fk", fe / 1000.0);
        } else {
            displayText = String.valueOf(fe);
        }

        fontRenderer.drawStringWithShadow(displayText,
                slot.xPos + 17 - fontRenderer.getStringWidth(displayText),
                slot.yPos + 8 - fontRenderer.FONT_HEIGHT / 2,
                0xFFFFFF);

        return true;
    }
}