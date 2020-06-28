package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.path.interpolator.SmoothInterpolator;
import com.mrcrayfish.director.screen.widget.ImprovedSlider;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

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
        super(new TranslationTextComponent("director.title.adjust_curve"), parent);
        this.point = point;
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        SmoothInterpolator.Properties properties = (SmoothInterpolator.Properties) this.point.getProperties();
        Slider slider = new ImprovedSlider(0, 0, 100, 20, "Smoothness: ", "", 0.0, 3.0, properties.getSmoothness(), true, true, slider1 -> properties.setSmoothness(MathHelper.clamp(slider1.getValue(), 0.0, 3.0)))
        {
            @Override
            public void onRelease(double mouseX, double mouseY)
            {
                super.onRelease(mouseX, mouseY);
                PathManager.get().updateLengthAndSteps();
            }
        };
        widgets.add(slider);

        widgets.add(Icons.RESET.createButton(0, 0, button -> {
            slider.setValue(1.0);
            slider.updateSlider();
            properties.setSmoothness(1.0);
            PathManager.get().updateLengthAndSteps();
        }).setDescription("director.button.reset_smoothness"));
    }
}
