package com.mrcrayfish.director.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.director.Director;
import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;

import net.minecraft.client.gui.components.Button.OnPress;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation(Director.ID, "textures/gui/icons.png");

    private Icons icon;
    private String description;

    public IconButton(int x, int y, int width, int height, Icons icon, OnPress pressable)
    {
        super(x, y, width, height, TextComponent.EMPTY, pressable);
        this.icon = icon;
    }

    public IconButton setDescription(String description)
    {
        this.description = description;
        return this;
    }

    public String getDescription()
    {
        return this.description;
    }

    public IconButton setIcon(@Nullable Icons icon)
    {
        this.icon = icon;
        return this;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
        Minecraft minecraft = Minecraft.getInstance();
        Font fontRenderer = minecraft.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        int combinedWidth = this.icon != null ? 10 : 0;
        String message = this.getMessage().getContents().trim();
        if(!message.isEmpty())
        {
            combinedWidth += fontRenderer.width(message);
            if(this.icon != null)
            {
                combinedWidth += 4;
            }
        }
        if(this.icon != null)
        {
            ScreenUtil.drawTexturedRect(this.x + this.width / 2 - combinedWidth / 2, this.y + 5, this.icon.getU(), this.icon.getV(), 10, 10, 10, 10);
        }
        if(!message.isEmpty())
        {
            fontRenderer.drawShadow(matrixStack, message, this.x + this.width / 2 - combinedWidth / 2 + 10 + (this.icon == null ? 0 : 4), this.y + 6, 0xFFFFFF);
        }
    }
}
