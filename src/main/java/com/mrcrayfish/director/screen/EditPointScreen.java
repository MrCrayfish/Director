package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * Author: MrCrayfish
 */
public class EditPointScreen extends Screen
{
    private PathPoint point;

    public EditPointScreen(PathPoint point)
    {
        super(new TranslationTextComponent("director.title.edit_point"));
        this.point = point;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        super.render(mouseX, mouseY, partialTicks);
        int startX = (this.width - 100) / 2;
        int startY = (this.height - 100) / 2;
        ScreenUtil.drawWindow(startX, startY, 100, 100);
    }
}
