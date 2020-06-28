package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.interpolator.InterpolateType;
import com.mrcrayfish.director.screen.widget.EnumButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathSettingScreen extends AbstractMenuScreen
{
    protected PathSettingScreen(@Nullable Screen parent)
    {
        super(new TranslationTextComponent("director.title.path_settings"), parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void loadWidgets(List<Widget> widgets)
    {
        widgets.add(new EnumButton<>(0, 0, 100, 20, InterpolateType.class, PathManager.get().getPositionInterpolator(), button -> {
            PathManager.get().setPositionInterpolator(((EnumButton<InterpolateType>) button).getCurrentEnum());
            PathManager.get().updateLengthAndSteps();
        }).setMessageKey("director.button.position_interpolate"));

        widgets.add(new EnumButton<>(0, 0, 100, 20, InterpolateType.class, PathManager.get().getRotationInterpolator(), button -> {
            PathManager.get().setRotationInterpolator(((EnumButton<InterpolateType>) button).getCurrentEnum());
            PathManager.get().updateLengthAndSteps();
        }).setMessageKey("director.button.rotation_interpolate"));
    }
}
