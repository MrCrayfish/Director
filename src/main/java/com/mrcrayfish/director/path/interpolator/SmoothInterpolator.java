package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.path.PathPoint;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * A smooth interpolator by using the hermite algorithm. Hermite allows for controls on points,
 * however in this version they are automatically generated based on neighbouring points.
 * <p>
 * Based on work by Nils Pipenbrinck (https://www.cubic.org/docs/hermite.htm)
 *
 * @author MrCrayfish
 */
public class SmoothInterpolator extends AbstractInterpolator
{
    public SmoothInterpolator(List<PathPoint> points)
    {
        super(points);
    }

    @Override
    public Vec3d pos(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        double pX = this.apply(index, p1.getX(), p2.getX(), p3.getX(), p4.getX(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
        double pY = this.apply(index, p1.getY(), p2.getY(), p3.getY(), p4.getY(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
        double pZ = this.apply(index, p1.getZ(), p2.getZ(), p3.getZ(), p4.getZ(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
        return new Vec3d(pX, pY, pZ);
    }

    @Override
    public float pitch(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getPitch(), p2.getPitch(), p3.getPitch(), p4.getPitch(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
    }

    @Override
    public float yaw(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        //Left this here just in case I need it in the future. I tested without it though and it's working fine.
        //float yawDistance = MathHelper.wrapSubtractDegrees((float) p1.getYaw(), (float) p2.getYaw());
        return (float) this.apply(index, p1.getYaw(), p2.getYaw(), p3.getYaw(), p4.getYaw(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
    }

    @Override
    public float roll(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getRoll(), p2.getRoll(), p3.getRoll(), p4.getRoll(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
    }

    @Override
    public double fov(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return this.apply(index, p1.getFov(), p2.getFov(), p3.getFov(), p4.getFov(), p1.getDuration(), p2.getDuration(), p3.getDuration(), progress);
    }

    private double apply(int index, double v1, double v2, double v3, double v4, int d1, int d2, int d3, double p)
    {
        double inTension = 0.0;
        double inContinuity = 0.0;
        double inBias = 0.0;
        double outTension = 0.0;
        double outContinuity = 0.0;
        double outBias = 0.0;
        double in = this.control(v2, v3, v4, true, index + 1 == this.points.size() - 1, inTension, inContinuity, inBias, d2, d3);
        double out = this.control(v1, v2, v3, false, index == 0 || index == this.points.size() - 1, outTension, outContinuity, outBias, d1, d2);
        return this.point(v2, v3, out, in, p);
    }

    /**
     * Calculates and returns a point between two different
     *
     * @param p1 the starting value
     * @param p2 the ending value
     * @param t1 the starting control/tangent
     * @param t2 the ending control/tangent
     * @param s a value between 0 and 1
     * @return a number
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
        /* Return zero for start and end points to produce a path */
        if(startEnd)
        {
            return 0.0;
        }
        double v1 = ((1.0 - tension) * (1.0 - continuity) * (1.0 + bias)) / 2.0;
        double v2 = ((1.0 - tension) * (1.0 + continuity) * (1.0 + bias)) / 2.0;
        double c = (in ? v1 : v2) * (p2 - p1) + (in ? v2 : v1) * (p3 - p2);
        return c * (!in ? (2.0 * ticks) / (previousTicks + ticks) : (2.0 * previousTicks) / (previousTicks + ticks));
    }
}