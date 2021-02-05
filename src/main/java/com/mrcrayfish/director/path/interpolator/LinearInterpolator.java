package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Author: MrCrayfish
 */
public class LinearInterpolator extends AbstractInterpolator
{
    public LinearInterpolator(InterpolateType interpolateType, PathType pathType)
    {
        super(interpolateType, pathType);
    }

    @Override
    public Vector3d pos(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        double pX = this.apply(p1.getX(), p2.getX(), progress);
        double pY = this.apply(p1.getY(), p2.getY(), progress);
        double pZ = this.apply(p1.getZ(), p2.getZ(), progress);
        return new Vector3d(pX, pY, pZ);
    }

    @Override
    public float pitch(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        return (float) this.apply(p1.getPitch(), p2.getPitch(), progress);
    }

    @Override
    public float yaw(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        float y1 = (float) MathHelper.wrapDegrees(p1.getYaw());
        float y2 = this.applyTargetYawAdjustment(y1, (float) MathHelper.wrapDegrees(p2.getYaw()));
        return (float) this.apply(y1, y2, progress);
    }

    @Override
    public float roll(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        return (float) this.apply(p1.getRoll(), p2.getRoll(), progress);
    }

    @Override
    public double fov(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        return this.apply(p1.getFov(), p2.getFov(), progress);
    }

    @Override
    public double length(int startIndex, int endIndex)
    {
        double length = 0;
        for(int i = startIndex; i < endIndex; i++)
        {
            Vector3d p1 = this.pos(i, 0F);
            Vector3d p2 = this.pos(i + 1, 0F);
            length += p1.distanceTo(p2);
        }
        return length;
    }

    @Override
    public float progress(int index, double distance, double length)
    {
        return (float) (distance / length);
    }

    private double apply(double v1, double v2, float progress)
    {
        return MathHelper.lerp(progress, v1, v2);
    }
}
