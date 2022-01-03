package com.mrcrayfish.director.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrcrayfish.director.Director;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

/**
 * Author: MrCrayfish
 */
public class ScreenUtil
{
    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation(Director.ID, "textures/gui/window.png");

    public static void drawWindow(int x, int y, Pair<Integer, Integer> dimensions)
    {
        drawWindow(x, y, dimensions.getLeft(), dimensions.getRight());
    }

    /**
     * Draws a dynamic window background. Instead of drawing a static window, this can be drawn at
     * any dimensions. This works similar to stretchable bitmaps in Android apps.
     *
     * @param x the x position
     * @param y the y position
     * @param width the width of the window including borders
     * @param height the height of the window including borders
     */
    public static void drawWindow(int x, int y, int width, int height)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, WINDOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        drawTexturedRect(x, y, 0, 0, 4, 4, 4, 4);                          /* Top left corner */
        drawTexturedRect(x + width - 4, y, 5, 0, 4, 4, 4, 4);              /* Top right corner */
        drawTexturedRect(x, y + height - 4, 0, 5, 4, 4, 4, 4);             /* Bottom left corner */
        drawTexturedRect(x + width - 4, y + height - 4, 5, 5, 4, 4, 4, 4); /* Bottom right corner */
        drawTexturedRect(x + 4, y, 4, 0, width - 8, 4, 1, 4);              /* Top border */
        drawTexturedRect(x + 4, y + height - 4, 4, 5, width - 8, 4, 1, 4); /* Bottom border */
        drawTexturedRect(x, y + 4, 0, 4, 4, height - 8, 4, 1);             /* Left border */
        drawTexturedRect(x + width - 4, y + 4, 5, 4, 4, height - 8, 4, 1); /* Right border */
        drawTexturedRect(x + 4, y + 4, 4, 4, width - 8, height - 8, 1, 1); /* Center */
    }

    public static Pair<Integer, Integer> getDimensionsForWindow(int contentWidth, int contentHeight)
    {
        return Pair.of(contentWidth + 8, contentHeight + 8);
    }

    public static void drawTexturedRect(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight)
    {
        float uScale = 1.0F / 256.0F;
        float vScale = 1.0F / 256.0F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(x, y + height, 0).uv(u * uScale, (v + textureHeight) * vScale).endVertex();
        buffer.vertex(x + width, y + height, 0).uv((u + textureWidth) * uScale, (v + textureHeight) * vScale).endVertex();
        buffer.vertex(x + width, y, 0).uv((u + textureWidth) * uScale, v * vScale).endVertex();
        buffer.vertex(x, y, 0).uv(u * uScale, v * vScale).endVertex();
        tesselator.end();
    }

    public static boolean isMouseWithin(int mouseX, int mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
