package v.akfz.cobe.event.listener;

import net.minecraft.client.Minecraft;
import v.akfz.aslib.event.api.EventPriority;
import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.aslib.event.impl.TickUpdater;
import v.akfz.cobe.core.animation.AsyncAnimationEngine;

public class TickListener implements Listener {
    @Subscribe(priority = EventPriority.HIGHEST)
    public void execute(TickUpdater event) {
        if (event.client) {
            if (Minecraft.getInstance().player != null && !AsyncAnimationEngine.getInstance().isRunning()) {
                AsyncAnimationEngine.getInstance().start();
            }
            AsyncAnimationEngine.getInstance().setGamePaused(Minecraft.getInstance().isPaused());
        }
    }
}
