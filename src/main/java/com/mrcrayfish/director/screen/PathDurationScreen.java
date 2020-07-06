package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.screen.widget.ImprovedSlider;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathDurationScreen extends AbstractMenuScreen
{
    protected PathDurationScreen(@Nullable Screen parent)
    {
        super(new TranslationTextComponent("director.title.path_duration"), parent);
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        Slider slider = new ImprovedSlider(0, 0, 200, 20, "Duration: ", "", 1, 1000, PathManager.instance().getDuration(), false, true, slider1 -> PathManager.instance().setDuration(slider1.getValueInt()))
        {
            @Override
            public void onRelease(double mouseX, double mouseY)
            {
                super.onRelease(mouseX, mouseY);
                PathManager.instance().updatePathPoints();
            }
        };
        widgets.add(slider);
    }
}
