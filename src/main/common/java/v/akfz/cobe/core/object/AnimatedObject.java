package v.akfz.cobe.core.object;

import org.joml.Matrix4f;
import v.akfz.cobe.core.animation.AnimationController;
import v.akfz.cobe.core.animation.AsyncAnimationEngine;
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.cache.HitboxCache;
import v.akfz.cobe.core.data.hitbox.HitboxData;
import v.akfz.cobe.core.hitbox.ActiveHitbox;

import java.util.List;
import java.util.Map;

// The main one in this company 😎
public interface AnimatedObject {
    String getStrId(); // entity have getID and its int and that breaking

    AnimationController getController();
    AnimatedObjectCache getCache();

    boolean playAnimation(String animation);
    default boolean shouldPlayAnimationsWhileGamePaused() {
        return false;
    }

    default List<ActiveHitbox> getServerHitboxes(String hitboxpath) {
        Map<String,Matrix4f> matrices = getCache().getAllBoneWorldMatrices();
        HitboxData data = HitboxCache.getFromCache(hitboxpath);
        return data != null ? ActiveHitbox.buildFromData(data, matrices) : List.of();
    }

    default void fastInit() {
        AsyncAnimationEngine.getInstance().register(this.getStrId(), this);
    }
    default void delete() {
        AsyncAnimationEngine.getInstance().unregister(this.getStrId());
    }
}
