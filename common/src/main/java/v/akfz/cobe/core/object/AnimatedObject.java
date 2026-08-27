package v.akfz.cobe.core.object;

import net.minecraft.world.phys.Vec3;
import v.akfz.cobe.core.animation.AnimationController;
import v.akfz.cobe.core.animation.AsyncAnimationEngine;
import v.akfz.cobe.core.cache.AnimatedObjectCache;

// The main one in this company 😎
public interface AnimatedObject {
    String getStrId(); // entity have getID and its int and that breaking

    AnimationController getController();

    AnimatedObjectCache getCache();

    Vec3 getPos();

    default boolean shouldPlayAnimationsWhileGamePaused() {
        return false;
    }

    default void fastInit() {
        AsyncAnimationEngine.getInstance().register(this.getStrId(), this);
    }
}
