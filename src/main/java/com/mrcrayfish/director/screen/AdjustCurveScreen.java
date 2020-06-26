package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.path.interpolator.SmoothInterpolator;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

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
        widgets.add(new ImprovedSlider(0, 0, 100, 20, "Smoothness: ", "", 0.0, 3.0, properties.getSmoothness(), true, true, slider -> properties.setSmoothness(MathHelper.clamp(slider.getValue(), 0.0, 3.0)))
        {
            @Override
            public void onRelease(double mouseX, double mouseY)
            {
                super.onRelease(mouseX, mouseY);
                PathManager.get().updateLengthAndSteps();
            }
        });
    }
}
