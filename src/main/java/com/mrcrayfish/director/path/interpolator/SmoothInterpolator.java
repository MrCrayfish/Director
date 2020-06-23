package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * A smooth interpolator by using the hermite algorithm. Hermite allows for controls on points,
 * however in this version they are automatically generated based on neighbouring points.
 * Based on work by Nils Pipenbrinck
 *
 * @link https://www.cubic.org/docs/hermite.htm
 */
public class SmoothInterpolator extends AbstractInterpolator
{
    public SmoothInterpolator(List<PathPoint> points)
    {
        super(points);
    }

    @Override
    public Vec3d get(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        boolean startEnd = index == 0 || index == this.points.size() - 1;
        boolean nextPointEnd = index + 1 == this.points.size() - 1;
        //Vec3d outControl = startEnd ? Vec3d.ZERO : new Vec3d(p3.getX() - p1.getX(), p3.getY() - p1.getY(), p3.getZ() - p1.getZ()).normalize().scale(distance(p2, p3));
        //Vec3d inControl = nextPointEnd ? Vec3d.ZERO : new Vec3d(p4.getX() - p2.getX(), p4.getY() - p2.getY(), p4.getZ() - p2.getZ()).normalize().scale(distance(p2, p3));
        double inTension = 0.0;
        double inContinuity = 0.0;
        double inBias = 0.0;
        double outTension = 0.0;
        double outContinuity = 0.0;
        double outBias = 0.0;
        double inX = control(p2.getX(), p3.getX(), p4.getX(), true, nextPointEnd, inTension, inContinuity, inBias, p2.getDuration(), p3.getDuration());
        double outX = control(p1.getX(), p2.getX(), p3.getX(), false, startEnd, outTension, outContinuity, outBias, p1.getDuration(), p2.getDuration());
        double inY = control(p2.getY(), p3.getY(), p4.getY(), true, nextPointEnd, inTension, inContinuity, inBias, p2.getDuration(), p3.getDuration());
        double outY = control(p1.getY(), p2.getY(), p3.getY(), false, startEnd, outTension, outContinuity, outBias, p1.getDuration(), p2.getDuration());
        double inZ = control(p2.getZ(), p3.getZ(), p4.getZ(), true, nextPointEnd, inTension, inContinuity, inBias, p2.getDuration(), p3.getDuration());
        double outZ = control(p1.getZ(), p2.getZ(), p3.getZ(), false, startEnd, outTension, outContinuity, outBias, p1.getDuration(), p2.getDuration());
        double pX = point(p2.getX(), p3.getX(), outX, inX, progress);
        double pY = point(p2.getY(), p3.getY(), outY, inY, progress);
        double pZ = point(p2.getZ(), p3.getZ(), outZ, inZ, progress);
        return new Vec3d(pX, pY, pZ);
    }

    @Override
    public double distance(int index, float progress)
    {
        float chunk = progress / 20F;
        double distance = 0;
        Vec3d startPos = this.get(index, 0F);
        for(int i = 0; i <= 20; i++)
        {
            Vec3d nextPos = this.get(index, i * chunk);
            distance += startPos.distanceTo(nextPos);
            startPos = nextPos;
        }
        return distance;
    }

    /**
     * Gets the distance between two path points
     *
     * @param a the starting point
     * @param b the ending point
     * @return the distance between the two points
     */
    private static double distance(PathPoint a, PathPoint b)
    {
        double deltaX = b.getX() - a.getX();
        double deltaY = b.getY() - a.getY();
        double deltaZ = b.getZ() - a.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Gets the position of the
     *
     * @param p1
     * @param p2
     * @param t1
     * @param t2
     * @param s
     * @return
     */
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

    /**
     * Creates a control suitable for the specified points. Allows for control over the tension,
     * continuity, and bias.
     *
     * @param p1         the previous point
     * @param p2         the current point
     * @param p3         the next point
     * @param in         true for incoming, false for outcoming
     * @param tension    the tension or how sharply the curve bends
     * @param continuity the continuity or how rapid is the change in speed and direction
     * @param bias       the bias or the direction of the curve as it passes through
     * @return a control for the tangent
     */
    private double control(double p1, double p2, double p3, boolean in, boolean startEnd, double tension, double continuity, double bias, int previousTicks, int ticks)
    {
        if(startEnd)
        {
            return 0.0;
        }
        double v1 = ((1.0 - tension) * (1.0 - continuity) * (1.0 + bias)) / 2.0;
        double v2 = ((1.0 - tension) * (1.0 + continuity) * (1.0 + bias)) / 2.0;
        double c = (in ? v1 : v2) * (p2 - p1) + (in ? v2 : v1) * (p3 - p2);
        c = c * (!in ? (2.0 * ticks) / (previousTicks + ticks) : (2.0 * previousTicks) / (previousTicks + ticks));
        return c;
    }
}