package v.akfz.cobe.core.data.keyframe;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.data.Transform;

import java.util.List;

public record KeyframeData(
        Transform transform,
        InterpolationType interpolation,
        Easing easing,
        @Nullable List<Float> bezierArgs
) {
}