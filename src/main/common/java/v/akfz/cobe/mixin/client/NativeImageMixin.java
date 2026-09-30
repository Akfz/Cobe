package v.akfz.cobe.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import v.akfz.cobe.mixinterface.NativeImageAccessor;

@Mixin(NativeImage.class)
public class NativeImageMixin implements NativeImageAccessor {
	@Shadow private long pixels;
	@Override
	public long getPixels() {
		return pixels;
	}
}
