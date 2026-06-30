package v.akfz.cobe.aengine.animation;

import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;

// Главный в этой компании 😎
public interface AnimatedObject {
    String getStrId(); // у энтити getID это int и оно ломается крч

    AnimationController getController();

    AnimatedObjectCache getCache();

    default boolean shouldPlayAnimationsWhileGamePaused() {
        return false;
    }
}