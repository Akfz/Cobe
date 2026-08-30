package v.akfz.cobe.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.cobe.core.animation.AsyncAnimationEngine;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void cobe$onClearLevel(Screen screen, CallbackInfo ci) {
        /*
        if (!AnimationCache.getFromCacheAnimation().isEmpty()) {
            AnimationCache.cleanCacheAnimations();
        }
         */
        if (AsyncAnimationEngine.getInstance().isRunning()) {
            AsyncAnimationEngine.getInstance().stop();
        }
    }
}
