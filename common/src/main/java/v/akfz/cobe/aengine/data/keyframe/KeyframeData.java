package v.akfz.cobe.aengine.data.keyframe;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.aengine.data.Transform;

import java.util.List;

public record KeyframeData(
        Transform transform,
        InterpolationType interpolation,
        @Nullable List<Float> bezierArgs
) {
    public KeyframeData(Transform transform) {
        this(transform, InterpolationType.LINEAR, null);
    }
}