package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public abstract class AbstractInterpolator
{
    protected List<PathPoint> points;

    public AbstractInterpolator(List<PathPoint> points)
    {
        this.points = points;
    }

    protected PathPoint getPoint(int index)
    {
        return this.points.get(MathHelper.clamp(index, 0, this.points.size() - 1));
    }

    public abstract Vec3d get(int index, float progress);

    public abstract double distance(int index, float progress);
}
