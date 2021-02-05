package com.mrcrayfish.director.screen.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.director.Director;
import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation(Director.ID, "textures/gui/icons.png");

    private Icons icon;
    private String description;

    public IconButton(int x, int y, int width, int height, Icons icon, IPressable pressable)
    {
        super(x, y, width, height, StringTextComponent.EMPTY, pressable);
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
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
        Minecraft minecraft = Minecraft.getInstance();
        FontRenderer fontRenderer = minecraft.fontRenderer;
        Minecraft.getInstance().getTextureManager().bindTexture(ICONS_TEXTURE);
        int combinedWidth = this.icon != null ? 10 : 0;
        String message = this.getMessage().getUnformattedComponentText().trim();
        if(!message.isEmpty())
        {
            combinedWidth += fontRenderer.getStringWidth(message);
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
            fontRenderer.drawStringWithShadow(matrixStack, message, this.x + this.width / 2 - combinedWidth / 2 + 10 + (this.icon == null ? 0 : 4), this.y + 6, 0xFFFFFF);
        }
    }
}
