package v.akfz.cobe.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.cobe.aengine.animation.calc.AsyncAnimationEngine;
import v.akfz.cobe.aengine.data.cache.AnimationCache;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    public void cobe$tick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !AsyncAnimationEngine.getInstance().isRunning()) {
            AsyncAnimationEngine.getInstance().start();
        }
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void cobe$onClearLevel(Screen screen, CallbackInfo ci) {
        if (!AnimationCache.getFromCacheAnimation().isEmpty()) {
            AnimationCache.cleanCacheAnimations();
        }
        if (AsyncAnimationEngine.getInstance().isRunning()) {
            AsyncAnimationEngine.getInstance().stop();
        }
    }
}