package com.mrcrayfish.director;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Utility class to help with hermite interpolation modified to work with {@link PathPoint}.
 * Based on work by Nils Pipenbrinck
 * @link https://www.cubic.org/docs/hermite.htm
 */
public class PathInterpolator
{
    private List<PathPoint> points;

    public PathInterpolator(List<PathPoint> points)
    {
        this.points = points;
    }

    public Vec3d get(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        boolean startEnd = index == 0 || index == this.points.size() - 1;
        boolean nextPointEnd = index + 1 == this.points.size() - 1;
        Vec3d outControl = startEnd ? Vec3d.ZERO : new Vec3d(p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ()).normalize().scale(distance(p2, p3));
        Vec3d inControl = nextPointEnd ? Vec3d.ZERO : new Vec3d(p4.getX() - p2.getX(), p4.getY() - p2.getY(), p4.getZ() - p2.getZ()).normalize().scale(distance(p2, p3));
        double pX = point(p2.getX(), p3.getX(), outControl.x, inControl.x, progress);
        double pY = point(p2.getY(), p3.getY(), outControl.y, inControl.y, progress);
        double pZ = point(p2.getZ(), p3.getZ(), outControl.z, inControl.z, progress);
        return new Vec3d(pX, pY, pZ);
    }

    private static double distance(PathPoint a, PathPoint b)
    {
        double deltaX = b.getX() - a.getX();
        double deltaY = b.getY() - a.getY();
        double deltaZ = b.getZ() - a.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public PathPoint getPoint(int index)
    {
        return this.points.get(MathHelper.clamp(index, 0, this.points.size() - 1));
    }

    public int getSize()
    {
        return this.points.size();
    }

    private double point(double p1, double p2, double t1, double t2, double s)
    {
        double ss = s * s;
        double sss = s * s * s;
        double a1 = 2 * sss - 3 * ss + 1;
        double a2 = -2 * sss + 3 * ss;
        double a3 = sss - 2 * ss + s;
        double a4 = sss - ss;
        return a1 * p1 + a2 * p2 + a3 * t1 + a4 * t2;
    }
}