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
    private double length;

    public PathInterpolator(List<PathPoint> points, int duration)
    {
        this.points = points;
        this.calculateLength(duration);
    }

    /* A estimate of the length */
    private void calculateLength(int duration)
    {
        double[] pathLengths = new double[this.points.size() - 1];
        double totalLength = 0;
        int segmentsPerPoint = 50;
        for(int i = 0; i < this.points.size() - 1; i++)
        {
            double pathLength = 0;
            for(int j = 0; j < segmentsPerPoint; j++)
            {
                float chunk = 1.0F / segmentsPerPoint;
                float progress = (float) j / (float) segmentsPerPoint;
                Vec3d p1 = this.get(i, progress);
                Vec3d p2 = this.get(i, progress + chunk);
                pathLength += p1.distanceTo(p2);
            }
            pathLengths[i] = pathLength;
            totalLength += pathLength;
        }
        this.length = totalLength;
        for(int i = 0; i < this.points.size() - 1; i++)
        {
            this.points.get(i).setDuration((int) (duration * (pathLengths[i] / totalLength) + 0.5));
        }
    }

    public Vec3d get(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index);
        PathPoint p2 = this.getPoint(index + 1);
        PathPoint p3 = this.getPoint(index + 2);
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();
        double deltaZ = p2.getZ() - p1.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        boolean startEnd = index == 0 || index == this.points.size() - 1;
        Vec3d outControl = startEnd ? Vec3d.ZERO : new Vec3d(deltaX, deltaY, deltaZ).normalize().scale(distance);
        Vec3d inControl = new Vec3d(p3.getX() - p2.getX(), p3.getY() - p2.getY(), p3.getZ() - p2.getZ()).normalize().scale(distance);
        double pX = point(p1.getX(), p2.getX(), outControl.x, inControl.x, progress);
        double pY = point(p1.getY(), p2.getY(), outControl.y, inControl.y, progress);
        double pZ = point(p1.getZ(), p2.getZ(), outControl.z, inControl.z, progress);
        return new Vec3d(pX, pY, pZ);
    }

    public PathPoint getPoint(int index)
    {
        return this.points.get(MathHelper.clamp(index, 0, this.points.size() - 1));
    }

    public int getSize()
    {
        return this.points.size();
    }

    public double getLength()
    {
        return length;
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