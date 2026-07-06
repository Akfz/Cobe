package v.akfz.cobe.aengine.data.cache;

import v.akfz.cobe.loader.json.animation.Animation;
import v.akfz.cobe.loader.json.animation.AnimationsData;

import java.util.HashMap;
import java.util.Map;

//Когда какой либо AnimatedObject хочет анимироваться, берется анимации из CACHED_ANIMATIONS,
// если в CACHED_ANIMATIONS нету этой анимации ищет в CACHE_ANIMATIONS и записывает в CACHED_ANIMATIONS

// CACHE_ANIMATIONS наполняется при каждой перезагрузке ресурсов, он сам ищет по json
public class AnimationCache {
    // Сюда так же можно вручную писать и добавлять анимации, без json
    // но CACHED_ANIMATIONS чистится после выхода из мира
    public static final Map<String, Animation> CACHED_ANIMATIONS = new HashMap<>();

    // это загружается при запуске и не чистится
    public static final Map<String, AnimationsData> CACHE_ANIMATIONS = new HashMap<>();
}
