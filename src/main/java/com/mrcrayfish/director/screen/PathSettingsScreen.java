package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathSettingsScreen extends AbstractMenuScreen
{
    public PathSettingsScreen()
    {
        super(new TranslationTextComponent("director.title.path_settings"), null);
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        widgets.add(Icons.IMPORT.createButton(0, 0, button -> {}).setDescription("director.button.import_path"));
        widgets.add(Icons.EXPORT.createButton(0, 0, button -> {}).setDescription("director.button.export_path"));
        widgets.add(Spacer.of(5));
        widgets.add(Icons.WRENCH.createButton(0, 0, button -> {}).setDescription("director.button.path_settings"));
        widgets.add(Spacer.of(5));
        widgets.add(Icons.BIN.createButton(0, 0, button -> {
            PathManager.get().deletePath();
        }).setDescription("director.button.delete_path"));
    }
}
