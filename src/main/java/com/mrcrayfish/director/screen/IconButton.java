package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Director;
import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;

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
        super(x, y, width, height, "", pressable);
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

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(mouseX, mouseY, partialTicks);
        Minecraft.getInstance().getTextureManager().bindTexture(ICONS_TEXTURE);
        ScreenUtil.drawTexturedRect(this.x + 5, this.y + 5, this.icon.getU(), this.icon.getV(), 10, 10, 10, 10);
    }
}
