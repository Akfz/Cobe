package v.akfz.cobe.aengine.data.cache;

import v.akfz.cobe.json.animation.Animation;

import java.util.HashMap;
import java.util.Map;

//Когда какой либо AnimatedObject хочет анимироваться, читается файл с его анимациями и добавляется сюда
public class AnimationCache {
    //Сюда так же можно вручную писать и добавлять анимации, без json
    // но CACHED_ANIMATIONS чистится после выхода из мира
    public static final Map<String, Animation> CACHED_ANIMATIONS = new HashMap<>();
}
