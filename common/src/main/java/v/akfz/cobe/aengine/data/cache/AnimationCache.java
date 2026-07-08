package v.akfz.cobe.aengine.data.cache;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.aengine.animation.event.AnimationEvent;
import v.akfz.cobe.loader.json.animation.Animation;
import v.akfz.cobe.loader.json.animation.AnimationsData;

import java.util.*;

//Когда какой либо AnimatedObject хочет анимироваться, берется анимации из CACHED_ANIMATIONS,
// если в CACHED_ANIMATIONS нету этой анимации ищет в CACHE_ANIMATIONS и записывает в CACHED_ANIMATIONS

// CACHE_ANIMATIONS наполняется при каждой перезагрузке ресурсов, он сам ищет по json
public class AnimationCache {
    // Сюда так же можно вручную писать и добавлять анимации, без json
    // но CACHED_ANIMATIONS чистится после выхода из мира
    private static final Map<String, Animation> CACHED_ANIMATIONS = new HashMap<>();
    public static void addCacheAnimation(Animation a, String name) {
        CACHED_ANIMATIONS.put(name,a);
    }
    @Nullable
    public static Animation getFromCacheAnimation(String name) {
        return CACHED_ANIMATIONS.get(name);
    }
    public static Map<String, Animation> getFromCacheAnimation() {
        return new HashMap<>(CACHED_ANIMATIONS);
    }

    private static final Map<String, List<AnimationEvent>> ANIMATION_EVENT = new HashMap<>(); // ивенты для анимаций, считай регистрация
    public static void addAnimationEvent(AnimationEvent event, String animationName) {
        if (ANIMATION_EVENT.get(animationName) != null) {
            ANIMATION_EVENT.get(animationName).add(event);
            return;
        }
        ANIMATION_EVENT.computeIfAbsent(animationName, k -> Collections.singletonList(event));
    }
    public static List<AnimationEvent> getAnimationEvents(String name) {
        return ANIMATION_EVENT.get(name);
    }

    // это загружается при запуске и не чистится
    private static final Map<String, AnimationsData> CACHE_ANIMATIONS_DATA = new HashMap<>();
    public static void addCacheAnimationData(AnimationsData a, String name) {
        CACHE_ANIMATIONS_DATA.put(name,a);
    }
    @Nullable
    public static AnimationsData getFromCacheAnimationData(String name) {
        return CACHE_ANIMATIONS_DATA.get(name);
    }
    public static Map<String, AnimationsData> getFromCacheAnimationData() {
        return new HashMap<>(CACHE_ANIMATIONS_DATA);
    }
}
