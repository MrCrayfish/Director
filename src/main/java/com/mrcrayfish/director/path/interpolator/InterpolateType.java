package com.mrcrayfish.director.path.interpolator;

/**
 * Author: MrCrayfish
 */
public enum InterpolateType
{
    LINEAR(new LinearInterpolator()),
    HERMITE(new SmoothInterpolator());

    private AbstractInterpolator interpolator;

    InterpolateType(AbstractInterpolator interpolator)
    {
        this.interpolator = interpolator;
    }

    public AbstractInterpolator get()
    {
        return this.interpolator;
    }
}
