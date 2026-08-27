package v.akfz.cobe.core.cache;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.animation.RuntimeAnimation;
import v.akfz.cobe.core.data.Animation;
import v.akfz.cobe.core.data.loader.json.animation.AnimationsData;
import v.akfz.cobe.core.event.AnimationEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationCache {

    private static volatile Map<String, Animation> CACHED_ANIMATIONS = Map.of();
    private static volatile Map<String, RuntimeAnimation> CACHED_RUNTIME_ANIMATIONS = Map.of();
    private static volatile Map<String, List<AnimationEvent>> ANIMATION_EVENTS = Map.of();
    private static volatile Map<String, AnimationsData> CACHED_ANIMATIONS_DATA = Map.of();

    public static synchronized void addCacheAnimation(Animation animation, String name) {
        if (animation == null || name == null) {
            return;
        }

        Map<String, Animation> animations = new HashMap<>(CACHED_ANIMATIONS);
        animations.put(name, animation);
        CACHED_ANIMATIONS = Map.copyOf(animations);

        Map<String, RuntimeAnimation> runtimeAnimations = new HashMap<>(CACHED_RUNTIME_ANIMATIONS);
        runtimeAnimations.put(name, new RuntimeAnimation(animation));
        CACHED_RUNTIME_ANIMATIONS = Map.copyOf(runtimeAnimations);
    }

    @Nullable
    public static Animation getFromCacheAnimation(String name) {
        if (name == null) {
            return null;
        }

        Animation animation = CACHED_ANIMATIONS.get(name);
        if (animation != null) {
            return animation;
        }

        Animation found = searchInAnimationData(name);
        if (found != null) {
            addCacheAnimation(found, name);
        }

        return found;
    }

    @Nullable
    public static RuntimeAnimation getRuntimeAnimation(String name) {
        if (name == null) return null;

        RuntimeAnimation ra = CACHED_RUNTIME_ANIMATIONS.get(name);
        if (ra != null) return ra;

        Animation anim = getFromCacheAnimation(name);
        if (anim != null) {
            addCacheAnimation(anim, name);
            return CACHED_RUNTIME_ANIMATIONS.get(name);
        }
        return null;
    }

    @Nullable
    private static Animation searchInAnimationData(String name) {
        for (AnimationsData data : CACHED_ANIMATIONS_DATA.values()) {
            if (data == null || data.animations == null) {
                continue;
            }

            for (Animation animation : data.animations) {
                if (animation != null && name.equals(animation.name())) {
                    return animation;
                }
            }
        }

        return null;
    }

    public static Map<String, Animation> getFromCacheAnimation() {
        return new HashMap<>(CACHED_ANIMATIONS);
    }

    public static synchronized void cleanCacheAnimations() {
        CACHED_ANIMATIONS = Map.of();
        CACHED_RUNTIME_ANIMATIONS = Map.of();
        ANIMATION_EVENTS = Map.of();
        CACHED_ANIMATIONS_DATA = Map.of();
    }

    public static synchronized void addAnimationEvent(AnimationEvent event, String animationName) {
        if (event == null || animationName == null) {
            return;
        }

        Map<String, List<AnimationEvent>> newMap = new HashMap<>(ANIMATION_EVENTS);

        List<AnimationEvent> oldList = newMap.getOrDefault(animationName, List.of());
        List<AnimationEvent> updatedList = new ArrayList<>(oldList);
        updatedList.add(event);

        newMap.put(animationName, List.copyOf(updatedList));
        ANIMATION_EVENTS = Map.copyOf(newMap);
    }

    public static List<AnimationEvent> getAnimationEvents(String name) {
        if (name == null) {
            return List.of();
        }
        return ANIMATION_EVENTS.getOrDefault(name, List.of());
    }

    public static synchronized void addCacheAnimationData(AnimationsData data, String name) {
        if (data == null || name == null) {
            return;
        }

        Map<String, AnimationsData> newMap = new HashMap<>(CACHED_ANIMATIONS_DATA);
        newMap.put(name, data);
        CACHED_ANIMATIONS_DATA = Map.copyOf(newMap);

        if (data.animations != null) {
            for (Animation animation : data.animations) {
                if (animation != null && animation.name() != null) {
                    addCacheAnimation(animation, animation.name());
                }
            }
        }
    }

    @Nullable
    public static AnimationsData getFromCacheAnimationData(String name) {
        if (name == null) {
            return null;
        }
        return CACHED_ANIMATIONS_DATA.get(name);
    }

    public static Map<String, AnimationsData> getFromCacheAnimationData() {
        return new HashMap<>(CACHED_ANIMATIONS_DATA);
    }
}