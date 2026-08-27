package v.akfz.cobe.core.animation;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.data.Animation;
import v.akfz.cobe.core.data.bone.BoneAData;
import v.akfz.cobe.core.data.keyframe.Easing;
import v.akfz.cobe.core.data.keyframe.FpsKeyframe;
import v.akfz.cobe.core.data.keyframe.InterpolationType;
import v.akfz.cobe.core.math.EasingMath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeAnimation {
    private final Animation animation;
    private final Map<String, BoneAData> bonesByName;

    public RuntimeAnimation(Animation animation) {
        if (animation == null) {
            throw new IllegalArgumentException("Animation cannot be null");
        }
        this.animation = animation;

        Map<String, BoneAData> map = new HashMap<>();
        if (animation.bones() != null) {
            for (BoneAData bone : animation.bones()) {
                if (bone != null && bone.boneName() != null) {
                    map.putIfAbsent(bone.boneName(), bone);
                }
            }
        }
        this.bonesByName = Map.copyOf(map);
    }

    public Animation getAnimation() {
        return animation;
    }

    @Nullable
    public BoneAData getBone(String boneName) {
        if (boneName == null) return null;
        return bonesByName.get(boneName);
    }

    public float getFpsAt(float timeSec) {
        List<FpsKeyframe> keys = animation.fpsKeyframes();

        if (keys == null || keys.isEmpty()) {
            return Math.max(animation.startFps(), 0);
        }

        float ms = timeSec * 1000.0f;

        FpsKeyframe first = keys.get(0);
        if (ms <= first.startTime()) {
            return Math.max(first.startValue(), 0.0f);
        }

        FpsKeyframe last = keys.get(keys.size() - 1);
        if (ms >= last.endTime()) {
            return Math.max(last.endValue(), 0.0f);
        }

        for (FpsKeyframe kf : keys) {
            if (ms >= kf.startTime() && ms <= kf.endTime()) {
                long dur = kf.endTime() - kf.startTime();
                if (dur <= 0L) {
                    return Math.max(kf.endValue(), 0.0f);
                }

                float alpha = (ms - kf.startTime()) / (float) dur;
                alpha = Math.max(0.0f, Math.min(1.0f, alpha));
                alpha = applyFpsAlpha(kf, alpha);

                float fps = kf.startValue() + (kf.endValue() - kf.startValue()) * alpha;
                return Math.max(fps, 0.0f);
            }
        }

        return Math.max(last.endValue(), 0.0f);
    }

    private static float applyFpsAlpha(FpsKeyframe kf, float alpha) {
        InterpolationType interpolation = kf.interpolation() == null
                ? InterpolationType.LINEAR
                : kf.interpolation();

        Easing easing = kf.easing() == null ? Easing.AUTOMATIC : kf.easing();

        switch (interpolation) {
            case STEP -> {
                return 0.0f;
            }
            case BEZIER -> {
                List<Float> args = kf.bezierArgs();
                if (args != null && args.size() == 4) {
                    return solveBezier(alpha, args.get(0), args.get(1), args.get(2), args.get(3));
                }
                return alpha;
            }
            default -> {
                return EasingMath.apply(alpha, interpolation, easing);
            }
        }
    }

    private static float solveBezier(float alpha, float x1, float y1, float x2, float y2) {
        if (alpha <= 0.0f) return 0.0f;
        if (alpha >= 1.0f) return 1.0f;

        float t = alpha;
        boolean converged = false;

        for (int i = 0; i < 8; i++) {
            float x = (3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t) - alpha;
            float derivative = 3 * (1 - t) * (1 - t) * x1 + 6 * (1 - t) * t * (x2 - x1) + 3 * t * t * (1 - x2);

            if (Math.abs(derivative) < 1e-5f) break;

            float nextT = t - x / derivative;
            if (Math.abs(nextT - t) < 1e-5f) {
                converged = true;
                t = nextT;
                break;
            }
            t = Math.max(0.0f, Math.min(1.0f, nextT));
        }

        if (!converged) return alpha;

        return 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t;
    }
}