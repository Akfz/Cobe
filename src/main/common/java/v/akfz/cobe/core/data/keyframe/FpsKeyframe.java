package v.akfz.cobe.core.data.keyframe;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record FpsKeyframe(
        long startTime,
        float startValue,
        long endTime,
        float endValue,
        @Nullable InterpolationType interpolation,
        @Nullable Easing easing,
        @Nullable List<Float> bezierArgs
) {
}