package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.screen.widget.IconButton;
import com.mrcrayfish.director.screen.widget.Spacer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathMenuScreen extends AbstractMenuScreen
{
    public PathMenuScreen()
    {
        super(new TranslationTextComponent("director.title.path_menu"), null);
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        widgets.add(Icons.IMPORT.createButton(0, 0, button -> {}).setDescription("director.button.import_path"));
        widgets.add(Icons.EXPORT.createButton(0, 0, button -> {}).setDescription("director.button.export_path"));
        widgets.add(Spacer.of(5));

        Icons visible = PathManager.get().isVisible() ? Icons.VISIBILITY_ON : Icons.VISIBILITY_OFF;
        widgets.add(visible.createButton(0, 0, button -> {
            PathManager.get().setVisible(!PathManager.get().isVisible());
            ((IconButton) button).setIcon(PathManager.get().isVisible() ? Icons.VISIBILITY_ON : Icons.VISIBILITY_OFF);
        }).setDescription("director.button.visibility"));

        widgets.add(Icons.WRENCH.createButton(0, 0, button -> {
            this.minecraft.displayGuiScreen(new PathSettingScreen(this));
        }).setDescription("director.button.path_settings"));

        widgets.add(Spacer.of(5));

        widgets.add(Icons.BIN.createButton(0, 0, button -> {
            PathManager.get().deletePath();
            this.onClose();
        }).setDescription("director.button.delete_path"));
    }
}
