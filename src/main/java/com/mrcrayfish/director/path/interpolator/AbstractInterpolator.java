package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.IProperties;
import com.mrcrayfish.director.path.PathManager;
import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public abstract class AbstractInterpolator
{
    private final InterpolateType type;
    private final PathType pathType;

    public AbstractInterpolator(InterpolateType type, PathType pathType)
    {
        this.type = type;
        this.pathType = pathType;
    }

    public InterpolateType getType()
    {
        return this.type;
    }

    public PathType getPathType()
    {
        return this.pathType;
    }

    protected PathPoint getPoint(int index)
    {
        List<PathPoint> points = PathManager.instance().getPoints();
        return points.get(MathHelper.clamp(index, 0, points.size() - 1));
    }

    protected int getPointCount()
    {
        return PathManager.instance().getPoints().size();
    }

    public abstract Vector3d pos(int index, float progress);

    public abstract float pitch(int index, float progress);

    public abstract float yaw(int index, float progress);

    public abstract float roll(int index, float progress);

    public abstract double fov(int index, float progress);

    public abstract double length(int startIndex, int endIndex);

    public abstract float progress(int index, double distance, double length);

    public void loadEditPointWidgets(List<Widget> widgets, PathPoint point, @Nullable Screen parent) {}

    public Supplier<IProperties> propertySupplier()
    {
        return () -> null;
    }

    protected float applyTargetYawAdjustment(float v1, float v2)
    {
        while(Math.abs(v2 - v1) > 180F)
        {
            float deltaYaw = v2 - v1;
            if(deltaYaw > 180F)
            {
                v2 -= 360F;
            }
            else if(deltaYaw < -180F)
            {
                v2 += 360F;
            }
        }
        return v2;
    }
}
