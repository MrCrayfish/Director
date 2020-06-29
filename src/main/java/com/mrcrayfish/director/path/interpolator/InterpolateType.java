package com.mrcrayfish.director.path.interpolator;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public enum InterpolateType
{
    LINEAR(LinearInterpolator::new),
    HERMITE(SmoothInterpolator::new);

    private BiFunction<InterpolateType, PathType, AbstractInterpolator> interpolator;

    InterpolateType(BiFunction<InterpolateType, PathType, AbstractInterpolator> interpolator)
    {
        this.interpolator = interpolator;
    }

    public AbstractInterpolator get(PathType pathType)
    {
        return this.interpolator.apply(this, pathType);
    }
}
