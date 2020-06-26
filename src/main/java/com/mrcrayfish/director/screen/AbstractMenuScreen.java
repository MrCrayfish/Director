package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public abstract class AbstractMenuScreen extends Screen
{
    private Screen parent;
    private int contentWidth;

    protected AbstractMenuScreen(ITextComponent titleIn, @Nullable Screen parent)
    {
        super(titleIn);
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        List<Widget> widgets = new ArrayList<>();
        if(this.parent != null)
        {
            widgets.add(Icons.LEFT_ARROW.createButton(0, 0, button -> this.minecraft.displayGuiScreen(this.parent)).setDescription("director.button.back"));
        }
        this.loadWidgets(widgets);

        int contentWidth = (widgets.size() - 1) * 2 + 4;
        for(Widget widget : widgets)
        {
            contentWidth += widget.getWidth();
        }
        this.contentWidth = contentWidth;

        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        int offset = 0;
        for(int i = 0; i < widgets.size(); i++)
        {
            Widget widget = widgets.get(i);
            widget.x = startX + 4 + 2 + offset;
            widget.y = startY + 4 + 2;
            offset += widget.getWidth() + 2;
            this.addButton(widget);
        }
    }

    protected abstract void loadWidgets(List<Widget> widgets);

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        ScreenUtil.drawWindow(startX, startY, dimensions);
        super.render(mouseX, mouseY, partialTicks);

        Widget hoveredWidget = null;
        for(Widget widget : this.buttons)
        {
            if(ScreenUtil.isMouseWithin(mouseX, mouseY, widget.x, widget.y, widget.getWidth(), widget.getHeight()))
            {
                hoveredWidget = widget;
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

    @Override
    public Optional<IGuiEventListener> getEventListenerForPos(double mouseX, double mouseY)
    {
        return super.getEventListenerForPos(mouseX, mouseY);
    }
}
