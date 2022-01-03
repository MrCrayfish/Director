package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.IProperties;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.screen.AdjustCurveScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

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
    public SmoothInterpolator(InterpolateType interpolateType, PathType pathType)
    {
        super(interpolateType, pathType);
    }

    @Override
    public Supplier<IProperties> propertySupplier()
    {
        return Properties::new;
    }

    @Override
    public void loadEditPointWidgets(List<Widget> widgets, PathPoint point, @Nullable Screen parent)
    {
        widgets.add(Icons.SHARE.createButton(0, 0, button -> {
            Minecraft.getInstance().setScreen(new AdjustCurveScreen(point, parent));
        }).setDescription("director.button.modify_curve"));
    }

    @Override
    public Vector3d pos(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        double pX = this.apply(index, p1.getX(), p2.getX(), p3.getX(), p4.getX(), p2, p3, progress);
        double pY = this.apply(index, p1.getY(), p2.getY(), p3.getY(), p4.getY(), p2, p3, progress);
        double pZ = this.apply(index, p1.getZ(), p2.getZ(), p3.getZ(), p4.getZ(), p2, p3, progress);
        return new Vector3d(pX, pY, pZ);
    }

    @Override
    public float pitch(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getPitch(), p2.getPitch(), p3.getPitch(), p4.getPitch(), p2, p3, progress);
    }

    @Override
    public float yaw(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        float y1 = (float) MathHelper.wrapDegrees(p1.getYaw());
        float y2 = this.applyTargetYawAdjustment(y1, (float) MathHelper.wrapDegrees(p2.getYaw()));
        float y3 = this.applyTargetYawAdjustment(y2, (float) MathHelper.wrapDegrees(p3.getYaw()));
        float y4 = this.applyTargetYawAdjustment(y3, (float) MathHelper.wrapDegrees(p4.getYaw()));
        return (float) this.apply(index, y1, y2, y3, y4, p2, p3, progress);
    }

    @Override
    public float roll(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getRoll(), p2.getRoll(), p3.getRoll(), p4.getRoll(), p2, p3, progress);
    }

    @Override
    public double fov(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return this.apply(index, p1.getFov(), p2.getFov(), p3.getFov(), p4.getFov(), p2, p3, progress);
    }

    @Override
    public double length(int startIndex, int endIndex)
    {
        /* A simple method to approximate the length between two points. There is no formula to instance
         * the length of a hermite spline, so this is the next best thing. */
        int start = Math.min(startIndex, endIndex);
        int end = Math.max(startIndex, endIndex);
        if(start < 0 && end >= this.getPointCount())
        {
            return 0.0;
        }
        int count = 100;
        double pathLength = 0.0;
        for(int i = startIndex; i < endIndex; i++)
        {
            for(int j = 0; j < count; j++)
            {
                float step = 1.0F / count;
                float progress = (float) j / (float) count;
                Vector3d p1 = this.pos(i, progress);
                Vector3d p2 = this.pos(i, progress + step);
                pathLength += p1.distanceTo(p2);
            }
        }
        return pathLength;
    }

    @Override
    public float progress(int index, double distance, double length)
    {
        return this.getProgressForDistance(100, index, 0, distance, 0.0F, 1.0F);
    }

    /**
     * Gets the rough progress required to reach the target distance. This method uses recursive
     * subdivision to approximately find a suitable value, which to the user appears correct.
     *
     * @param limit          the maximum amount of times this method can call itself
     * @param index          the path index
     * @param startDistance  the starting distance of the search
     * @param targetDistance the target distance to find
     * @param startProgress  the start progress of the search
     * @param endProgress    the end progress of the search
     * @return an approximate progress value to obtain the target distance
     */
    private float getProgressForDistance(int limit, int index, double startDistance, double targetDistance, float startProgress, float endProgress)
    {
        if(limit <= 0)
        {
            return startProgress;
        }

        float tailProgress = 0;
        float headProgress = 0;
        double distance = startDistance;
        Vector3d lastPos = this.pos(index, startProgress);
        float step = (endProgress - startProgress) / 10F;
        for(int i = 0; i <= 10; i++)
        {
            float progress = i * step;
            Vector3d pos = this.pos(index, startProgress + progress);
            distance += pos.distanceTo(lastPos);
            lastPos = pos;
            if(distance < targetDistance)
            {
                tailProgress = progress;
                startDistance = distance;
            }
            else if(distance > targetDistance && headProgress == 0)
            {
                headProgress = progress;
            }
            if(Math.abs(targetDistance - distance) < 0.001)
            {
                return startProgress + progress;
            }
        }
        return this.getProgressForDistance(limit - 1, index, startDistance, targetDistance, startProgress + tailProgress, startProgress + headProgress);
    }

    /**
     * Applies the hermite algorithm to the specified values. The calculation requires at least four
     * values for it to produce a result.
     *
     * @param index the path point index (n)
     * @param v1    a value from the n - 1 path point
     * @param v2    a value from the n path point
     * @param v3    a value from the n + 1 path point
     * @param v4    a value from the n + 2 path point
     * @param p1    the instance of n path point
     * @param p2    the instance of n + 1 path point
     * @param p     the progress between n and n + 1 which should be from 0.0 to 1.0
     * @return a calculated hermite value
     */
    private double apply(int index, double v1, double v2, double v3, double v4, PathPoint p1, PathPoint p2, double p)
    {
        double inTension = 0.0;
        double inContinuity = 0.0;
        double inBias = 0.0;
        double outTension = 0.0;
        double outContinuity = 0.0;
        double outBias = 0.0;
        double s1 = ((Properties) p1.getProperties(this.getType(), this.getPathType())).getSmoothness();
        double s2 = ((Properties) p2.getProperties(this.getType(), this.getPathType())).getSmoothness();
        double in = this.control(v2, v3, v4, s2, true, index + 1 == this.getPointCount() - 1, inTension, inContinuity, inBias);
        double out = this.control(v1, v2, v3, s1, false, index == 0 || index == this.getPointCount() - 1, outTension, outContinuity, outBias);
        return this.point(v2, v3, out, in, p);
    }

    /**
     * Calculates and returns a point between two different
     *
     * @param p1 the starting value
     * @param p2 the ending value
     * @param t1 the starting control/tangent
     * @param t2 the ending control/tangent
     * @param s  a value between 0 and 1
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
    private double control(double p1, double p2, double p3, double s1, boolean in, boolean startEnd, double tension, double continuity, double bias)
    {
        /* Return zero for start and end points to produce a better looking path */
        if(startEnd)
        {
            return 0.0;
        }
        double v1 = ((1.0 - tension) * (1.0 - continuity) * (1.0 + bias)) / 2.0;
        double v2 = ((1.0 - tension) * (1.0 + continuity) * (1.0 + bias)) / 2.0;
        return ((in ? v1 : v2) * (p2 - p1) + (in ? v2 : v1) * (p3 - p2)) * s1;
    }

    public static class Properties implements IProperties
    {
        private double smoothness = 1.0;

        /**
         * Sets the smoothness of the path point. The higher the value, the more smoother it
         * moves through the path point.
         *
         * @param smoothness the smoothness of this path point
         */
        public void setSmoothness(double smoothness)
        {
            this.smoothness = smoothness;
        }

        /**
         * Gets the smoothness of the path point or how tightly it moves through the path point
         *
         * @return the smoothness
         */
        public double getSmoothness()
        {
            return this.smoothness;
        }
    }
}