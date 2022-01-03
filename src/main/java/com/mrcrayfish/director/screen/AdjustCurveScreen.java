package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.path.interpolator.PathType;
import com.mrcrayfish.director.path.interpolator.SmoothInterpolator;
import com.mrcrayfish.director.screen.widget.ImprovedSlider;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.Slider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class AdjustCurveScreen extends AbstractMenuScreen
{
    private PathPoint point;

    public AdjustCurveScreen(PathPoint point, @Nullable Screen parent)
    {
        super(new TranslatableComponent("director.title.adjust_curve"), parent);
        this.point = point;
    }

    @Override
    protected void loadWidgets(List<AbstractWidget> widgets)
    {
        SmoothInterpolator.Properties properties = (SmoothInterpolator.Properties) this.point.getPositionProperties(PathManager.instance().getInterpolator(PathType.POSITION).getType());
        Slider slider = new ImprovedSlider(0, 0, 100, 20, "Smoothness: ", "", 0.0, 3.0, properties.getSmoothness(), true, true, slider1 -> properties.setSmoothness(Mth.clamp(slider1.getValue(), 0.0, 3.0)))
        {
            @Override
            public void onRelease(double mouseX, double mouseY)
            {
                super.onRelease(mouseX, mouseY);
                PathManager.instance().updatePathPoints();
            }
        };
        widgets.add(slider);

        widgets.add(Icons.RESET.createButton(0, 0, button -> {
            slider.setValue(1.0);
            slider.updateSlider();
            properties.setSmoothness(1.0);
            PathManager.instance().updatePathPoints();
        }).setDescription("director.button.reset_smoothness"));
    }
}
