package com.mrcrayfish.director.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.screen.widget.IconButton;
import com.mrcrayfish.director.screen.widget.Spacer;
import com.mrcrayfish.director.util.ScreenUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public abstract class AbstractMenuScreen extends Screen
{
    private Screen parent;
    private int contentWidth;

    protected AbstractMenuScreen(Component titleIn, @Nullable Screen parent)
    {
        super(titleIn);
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        this.minecraft.player.displayClientMessage(new TextComponent(""), true);
        this.minecraft.player.setDeltaMovement(0, 0, 0);

        List<AbstractWidget> widgets = new ArrayList<>();
        if(this.parent != null)
        {
            widgets.add(Icons.LEFT_ARROW.createButton(0, 0, button -> this.minecraft.setScreen(this.parent)).setDescription("director.button.back"));
            widgets.add(Spacer.of(5));
        }
        this.loadWidgets(widgets);

        int contentWidth = (widgets.size() - 1) * 2 + 4;
        for(AbstractWidget widget : widgets)
        {
            contentWidth += widget.getWidth();
        }
        this.contentWidth = contentWidth;

        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        int offset = 0;
        for(AbstractWidget widget : widgets)
        {
            widget.x = startX + 4 + 2 + offset;
            widget.y = startY + 4 + 2;
            offset += widget.getWidth() + 2;
            this.addRenderableWidget(widget);
        }
    }

    protected abstract void loadWidgets(List<AbstractWidget> widgets);

    @Override
    public void tick()
    {
        /* Close any menu if can't play a path*/
        if(!PathManager.instance().isPlayerValidDirector())
        {
            this.removed();
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.fillGradient(matrixStack, 0, this.height / 2, this.width, this.height, 0x00000000, 0xAA000000);

        Pair<Integer, Integer> dimensions = ScreenUtil.getDimensionsForWindow(this.contentWidth, 24);
        int startX = (this.width - dimensions.getLeft()) / 2;
        int startY = (this.height - dimensions.getRight()) - dimensions.getRight() / 2;
        ScreenUtil.drawWindow(startX, startY, dimensions);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        IconButton hoveredWidget = null;
        for(GuiEventListener widget : this.children())
        {
            if(widget instanceof IconButton iconButton)
            {
                if(ScreenUtil.isMouseWithin(mouseX, mouseY, iconButton.x, iconButton.y, iconButton.getWidth(), iconButton.getHeight()))
                {
                    hoveredWidget = iconButton;
                    break;
                }
            }
        }

        if(hoveredWidget != null)
        {
            String descriptionKey = hoveredWidget.getDescription();
            String description = I18n.get(descriptionKey);
            int width = this.minecraft.font.width(description);
            drawString(matrixStack, this.minecraft.font, description, this.width / 2 - width / 2, startY - 12, 0xFFFFFF);
        }
    }
}
