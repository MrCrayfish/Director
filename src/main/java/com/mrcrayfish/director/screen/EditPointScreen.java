package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: mrcrayfish
 */
public class EditPointScreen extends Screen
{
    private PathPoint point;
    private int contentWidth;

    public EditPointScreen(PathPoint point)
    {
        super(new TranslationTextComponent("director.title.edit_point"));
        this.point = point;
    }

    @Override
    protected void init()
    {
        List<AbstractButton> buttons = new ArrayList<>();
        buttons.add(Icons.MARKER.createButton(0, 0, button -> {
            PathManager.get().reposition(this.point);
            this.onClose();
        }).setDescription("director.button.reposition"));
        buttons.add(Icons.PENCIL.createButton(0, 0, button -> {}).setDescription("director.button.change_values"));
        buttons.add(Icons.SHARE.createButton(0, 0, button -> {}).setDescription("director.button.modify_curve"));
        buttons.add(Icons.BIN.createButton(0, 0, button -> {
            PathManager.get().delete(this.point);
            this.onClose();
        }).setDescription("director.button.delete"));

        this.contentWidth = buttons.size() * 20 + (buttons.size() - 1) * 2 + 4;
        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        for(int i = 0; i < buttons.size(); i++)
        {
            AbstractButton button = buttons.get(i);
            button.x = startX + i * 22 + 4 + 2;
            button.y = startY + 4 + 2;
            this.addButton(button);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        ScreenUtil.drawWindow(startX, startY, dimensions);
        super.render(mouseX, mouseY, partialTicks);

        Widget hoveredWidget = null;
        for(Widget button : this.buttons)
        {
            if(ScreenUtil.isMouseWithin(mouseX, mouseY, button.x, button.y, button.getWidth(), button.getHeight()))
            {
                hoveredWidget = button;
                break;
            }
        }

        if(hoveredWidget instanceof IconButton)
        {
            String descriptionKey = ((IconButton) hoveredWidget).getDescription();
            String description = I18n.format(descriptionKey);
            int width = this.minecraft.fontRenderer.getStringWidth(description);
            this.drawString(this.minecraft.fontRenderer, description, this.width / 2 - width / 2, startY - 12, 0xFFFFFF);
        }
    }
}
