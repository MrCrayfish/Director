package com.mrcrayfish.director.path.interpolator;

import com.mrcrayfish.director.Icons;
import com.mrcrayfish.director.path.IProperties;
import com.mrcrayfish.director.path.PathPoint;
import com.mrcrayfish.director.screen.AdjustCurveScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.Vec3d;

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
    @Override
    public Supplier<IProperties> propertySupplier()
    {
        return Properties::new;
    }

    @Override
    public void loadEditPointWidgets(List<Widget> widgets, PathPoint point, @Nullable Screen parent)
    {
        widgets.add(Icons.SHARE.createButton(0, 0, button -> {
            Minecraft.getInstance().displayGuiScreen(new AdjustCurveScreen(point, parent));
        }).setDescription("director.button.modify_curve"));
    }

    @Override
    public Vec3d pos(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        double pX = this.apply(index, p1.getX(), p2.getX(), p3.getX(), p4.getX(), p1, p2, p3, p4, progress);
        double pY = this.apply(index, p1.getY(), p2.getY(), p3.getY(), p4.getY(), p1, p2, p3, p4, progress);
        double pZ = this.apply(index, p1.getZ(), p2.getZ(), p3.getZ(), p4.getZ(), p1, p2, p3, p4, progress);
        return new Vec3d(pX, pY, pZ);
    }

    @Override
    public float pitch(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getPitch(), p2.getPitch(), p3.getPitch(), p4.getPitch(), p1, p2, p3, p4, progress);
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
        return (float) this.apply(index, p1.getYaw(), p2.getYaw(), p3.getYaw(), p4.getYaw(), p1, p2, p3, p4, progress);
        //return (float) (p2.getYaw() + (p3.getYaw() - p2.getYaw()) * progress);
    }

    @Override
    public float roll(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return (float) this.apply(index, p1.getRoll(), p2.getRoll(), p3.getRoll(), p4.getRoll(), p1, p2, p3, p4, progress);
    }

    @Override
    public double fov(int index, float progress)
    {
        PathPoint p1 = this.getPoint(index - 1);
        PathPoint p2 = this.getPoint(index);
        PathPoint p3 = this.getPoint(index + 1);
        PathPoint p4 = this.getPoint(index + 2);
        return this.apply(index, p1.getFov(), p2.getFov(), p3.getFov(), p4.getFov(), p1, p2, p3, p4, progress);
    }

    @Override
    public double length(int startIndex, int endIndex)
    {
        /* A simple method to approximate the length between two points. There is no formula to get
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
                Vec3d p1 = this.pos(i, progress);
                Vec3d p2 = this.pos(i, progress + step);
                pathLength += p1.distanceTo(p2);
            }
        }
        return pathLength;
    }

    private double apply(int index, double v1, double v2, double v3, double v4, PathPoint p1, PathPoint p2, PathPoint p3, PathPoint p4, double p)
    {
        double inTension = 0.0;
        double inContinuity = 0.0;
        double inBias = 0.0;
        double outTension = 0.0;
        double outContinuity = 0.0;
        double outBias = 0.0;
        double s1 = ((Properties) p1.getProperties()).getSmoothness();
        double s2 = ((Properties) p2.getProperties()).getSmoothness();
        double s3 = ((Properties) p3.getProperties()).getSmoothness();
        double s4 = ((Properties) p4.getProperties()).getSmoothness();
        double in = this.control(v2, v3, v4, s2, s3, s4, true, index + 1 == this.getPointCount() - 1, inTension, inContinuity, inBias, 0, 0);
        double out = this.control(v1, v2, v3, s1, s2, s3, false, index == 0 || index == this.getPointCount() - 1, outTension, outContinuity, outBias, 0, 0);
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
    private double control(double p1, double p2, double p3, double s1, double s2, double s3, boolean in, boolean startEnd, double tension, double continuity, double bias, int previousTicks, int ticks)
    {
        /* Return zero for start and end points to produce a path */
        if(startEnd)
        {
            return 0.0;
        }
        double v1 = ((1.0 - tension) * (1.0 - continuity) * (1.0 + bias)) / 2.0;
        double v2 = ((1.0 - tension) * (1.0 + continuity) * (1.0 + bias)) / 2.0;
        return ((in ? v1 : v2) * (p2 - p1) + (in ? v2 : v1) * (p3 - p2)) * s2;
        //return c * (in ? (2.0 * previousTicks) / (previousTicks + ticks) : (2.0 * ticks) / (previousTicks + ticks));
    }

    public static class Properties implements IProperties
    {
        private double smoothness = 1.0;

        public void setSmoothness(double smoothness)
        {
            this.smoothness = smoothness;
        }

        public double getSmoothness()
        {
            return this.smoothness;
        }
    }
}