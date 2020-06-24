package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

import java.util.List;

/**
 * Author: mrcrayfish
 */
public class EditPointScreen extends AbstractMenuScreen
{
    private PathPoint point;

    public EditPointScreen(PathPoint point)
    {
        super(new TranslationTextComponent("director.title.edit_point"), null);
        this.point = point;
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        widgets.add(Icons.MARKER.createButton(0, 0, button -> {
            PathManager.get().reposition(this.point);
            this.onClose();
        }).setDescription("director.button.reposition"));

        widgets.add(Icons.PENCIL.createButton(0, 0, button -> {}).setDescription("director.button.change_values"));

        widgets.add(Icons.SHARE.createButton(0, 0, button -> {
            this.minecraft.displayGuiScreen(new AdjustCurveScreen(this));
        }).setDescription("director.button.modify_curve"));

        widgets.add(Icons.PLUS.createButton(0, 0, button -> {
            PathManager.get().insertAfter(this.point);
            this.onClose();
        }).setDescription("director.button.insert_after"));

        widgets.add(Icons.BIN.createButton(0, 0, button -> {
            PathManager.get().delete(this.point);
            this.onClose();
        }).setDescription("director.button.delete"));
    }
}
