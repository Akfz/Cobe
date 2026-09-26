package v.akfz.cobe.core.data.keyframe;

public record Keyframe(
        long startValue,
        long endValue,
        int frame,
        float fps,
        KeyframeData data
) {
}
