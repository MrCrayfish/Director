package com.mrcrayfish.director.screen;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.interpolator.InterpolateType;
import com.mrcrayfish.director.path.interpolator.PathType;
import com.mrcrayfish.director.screen.widget.EnumButton;
import com.mrcrayfish.director.screen.widget.IconButton;
import com.mrcrayfish.director.screen.widget.Spacer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathSettingScreen extends AbstractMenuScreen
{
    protected PathSettingScreen(@Nullable Screen parent)
    {
        super(new TranslatableComponent("director.title.path_settings"), parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void loadWidgets(List<AbstractWidget> widgets)
    {
        widgets.add(new IconButton(0, 0, 20, 20, Icons.CLOCK, buttons -> {
            this.minecraft.setScreen(new PathDurationScreen(this));
        }).setDescription("director.button.path_duration"));

        widgets.add(Spacer.of(5));

        widgets.add(new EnumButton<>(0, 0, 60, 20, InterpolateType.class, PathManager.instance().getInterpolator(PathType.POSITION).getType(), button -> {
            PathManager.instance().setInterpolator(((EnumButton<InterpolateType>) button).getCurrentEnum(), PathType.POSITION);
            PathManager.instance().updatePathPoints();
        }).setDescription("director.button.position_interpolate").setIcon(Icons.PATH));

        widgets.add(new EnumButton<>(0, 0, 60, 20, InterpolateType.class, PathManager.instance().getInterpolator(PathType.ROTATION).getType(), button -> {
            PathManager.instance().setInterpolator(((EnumButton<InterpolateType>) button).getCurrentEnum(), PathType.ROTATION);
            PathManager.instance().updatePathPoints();
        }).setDescription("director.button.rotation_interpolate").setIcon(Icons.ROTATION));
    }
}
