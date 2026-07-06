package v.akfz.cobe.aengine.animation.keyframe.event;

import v.akfz.cobe.aengine.animation.AnimatedObject;

//TODO в начале и конце keyframe выполнять runnable или ченибуть
public interface KeyframeEvent {
    void onStart(AnimatedObject object);
}
