package com.mrcrayfish.director.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class AdjustCurveScreen extends AbstractMenuScreen
{
    protected AdjustCurveScreen(@Nullable Screen parent)
    {
        super(new TranslationTextComponent("director.title.adjust_curve"), parent);
    }

    @Override
    protected void loadWidgets(List<Widget> widgets)
    {
        widgets.add(new Slider(0, 0, 100, 20, "Smoothness:", "", -1.0, 1.0, 0.0, true, true, slider -> {}));
    }
}
