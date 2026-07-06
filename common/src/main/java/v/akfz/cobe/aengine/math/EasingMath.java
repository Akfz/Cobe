package v.akfz.cobe.aengine.math;

import v.akfz.cobe.aengine.data.keyframe.Easing;
import v.akfz.cobe.aengine.data.keyframe.InterpolationType;

public class EasingMath {

    public static float apply(float t, InterpolationType type, Easing easing) {
        if (type == InterpolationType.LINEAR) return t;
        if (type == InterpolationType.STEP) return t >= 1.0f ? 1.0f : 0.0f;
        if (type == InterpolationType.BEZIER) return t;

        Easing resolvedEasing = easing;
        if (easing == Easing.AUTOMATIC) {
            resolvedEasing = isDynamicEffect(type) ? Easing.EASE_OUT : Easing.EASE_IN;
        }

        switch (resolvedEasing) {
            case EASE_IN -> { return easeIn(t, type); }
            case EASE_OUT -> { return easeOut(t, type); }
            case EASE_IN_OUT -> { return easeInOut(t, type); }
            default -> { return easeOut(t, type); }
        }
    }

    private static boolean isDynamicEffect(InterpolationType type) {
        return type == InterpolationType.BACK || type == InterpolationType.BOUNCE || type == InterpolationType.ELASTIC;
    }

    private static float easeIn(float t, InterpolationType type) {
        return switch (type) {
            case SINUSOIDAL -> 1.0f - (float) Math.cos((t * Math.PI) / 2.0);
            case QUADRATIC   -> t * t;
            case CUBIC       -> t * t * t;
            case QUARTIC     -> t * t * t * t;
            case QUINTIC     -> t * t * t * t * t;
            case EXPONENTIAL -> t == 0.0f ? 0.0f : (float) Math.pow(2.0, 10.0 * (t - 1.0));
            case CIRCULAR    -> 1.0f - (float) Math.sqrt(1.0 - t * t);
            case BACK        -> {
                float s = 1.70158f;
                yield t * t * ((s + 1.0f) * t - s);
            }
            case BOUNCE      -> 1.0f - easeOut(1.0f - t, InterpolationType.BOUNCE);
            case ELASTIC     -> {
                if (t == 0.0f) yield 0.0f;
                if (t == 1.0f) yield 1.0f;
                yield -((float) Math.pow(2.0, 10.0 * t - 10.0)) * (float) Math.sin((t * 10.0f - 10.75f) * ((2.0 * Math.PI) / 3.0));
            }
            default -> t;
        };
    }

    private static float easeOut(float t, InterpolationType type) {
        return switch (type) {
            case SINUSOIDAL -> (float) Math.sin((t * Math.PI) / 2.0);
            case QUADRATIC   -> 1.0f - (1.0f - t) * (1.0f - t);
            case CUBIC       -> 1.0f - (float) Math.pow(1.0 - t, 3.0);
            case QUARTIC     -> 1.0f - (float) Math.pow(1.0 - t, 4.0);
            case QUINTIC     -> 1.0f - (float) Math.pow(1.0 - t, 5.0);
            case EXPONENTIAL -> t == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
            case CIRCULAR    -> (float) Math.sqrt(1.0 - (t - 1.0) * (t - 1.0));
            case BACK        -> {
                float s = 1.70158f;
                float t1 = t - 1.0f;
                yield t1 * t1 * ((s + 1.0f) * t1 + s) + 1.0f;
            }
            case BOUNCE      -> {
                float n1 = 7.5625f;
                float d1 = 2.75f;
                if (t < 1.0f / d1) {
                    yield n1 * t * t;
                } else if (t < 2.0f / d1) {
                    float t1 = t - 1.5f / d1;
                    yield n1 * t1 * t1 + 0.75f;
                } else if (t < 2.5f / d1) {
                    float t1 = t - 2.25f / d1;
                    yield n1 * t1 * t1 + 0.9375f;
                } else {
                    float t1 = t - 2.625f / d1;
                    yield n1 * t1 * t1 + 0.984375f;
                }
            }
            case ELASTIC     -> {
                if (t == 0.0f) yield 0.0f;
                if (t == 1.0f) yield 1.0f;
                yield (float) Math.pow(2.0, -10.0 * t) * (float) Math.sin((t * 10.0f - 0.75f) * ((2.0 * Math.PI) / 3.0)) + 1.0f;
            }
            default -> t;
        };
    }

    private static float easeInOut(float t, InterpolationType type) {
        if (t < 0.5f) {
            return easeIn(t * 2.0f, type) / 2.0f;
        } else {
            return easeOut(t * 2.0f - 1.0f, type) / 2.0f + 0.5f;
        }
    }
}