package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.screen.widget.IconButton;
import com.mrcrayfish.director.screen.widget.Spacer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathMenuScreen extends AbstractMenuScreen
{
    public PathMenuScreen()
    {
        super(new TranslatableComponent("director.title.path_menu"), null);
    }

    @Override
    protected void loadWidgets(List<AbstractWidget> widgets)
    {
        widgets.add(Icons.IMPORT.createButton(0, 0, button -> {}).setDescription("director.button.import_path"));
        widgets.add(Icons.EXPORT.createButton(0, 0, button -> {}).setDescription("director.button.export_path"));
        widgets.add(Spacer.of(5));

        Icons visible = PathManager.instance().isVisible() ? Icons.VISIBILITY_ON : Icons.VISIBILITY_OFF;
        widgets.add(visible.createButton(0, 0, button -> {
            PathManager.instance().setVisible(!PathManager.instance().isVisible());
            ((IconButton) button).setIcon(PathManager.instance().isVisible() ? Icons.VISIBILITY_ON : Icons.VISIBILITY_OFF);
        }).setDescription("director.button.visibility"));

        widgets.add(Icons.WRENCH.createButton(0, 0, button -> {
            this.minecraft.setScreen(new PathSettingScreen(this));
        }).setDescription("director.button.path_settings"));

        widgets.add(Spacer.of(5));

        widgets.add(Icons.BIN.createButton(0, 0, button -> {
            PathManager.instance().deletePath();
            this.removed();
        }).setDescription("director.button.delete_path"));
    }
}
