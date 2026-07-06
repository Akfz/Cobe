package v.akfz.cobe.aengine.data.keyframe;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.aengine.data.Transform;

import java.util.List;

public record KeyframeData(
        Transform transform,
        InterpolationType interpolation,
        Easing easing,
        @Nullable List<Float> bezierArgs
) {
}