package n.paradox.cobe.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.cobe.aengine.data.cache.AnimationCache;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    public void cobe$tick(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) {
            if (!AnimationCache.CACHED_ANIMATIONS.isEmpty()) {
                AnimationCache.CACHED_ANIMATIONS.clear();
            }
        }
    }
}
