package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.IProperties;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public abstract class AbstractInterpolator
{
    protected PathPoint getPoint(int index)
    {
        List<PathPoint> points = PathManager.get().getPoints();
        return points.get(MathHelper.clamp(index, 0, points.size() - 1));
    }

    protected int getPointCount()
    {
        return PathManager.get().getPoints().size();
    }

    public abstract Vec3d pos(int index, float progress);

    public abstract float pitch(int index, float progress);

    public abstract float yaw(int index, float progress);

    public abstract float roll(int index, float progress);

    public abstract double fov(int index, float progress);

    public abstract double length(int startIndex, int endIndex);

    public void loadEditPointWidgets(List<Widget> widgets, PathPoint point, @Nullable Screen parent) {}

    public Supplier<IProperties> propertySupplier()
    {
        return null;
    }
}
