package v.akfz.cobe.texture.video;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * How to use : VideoPlayerManager.getInstance().getOrCreatePlayer("textures/siuu.mp4", true, () -> {});
 */
public class VideoTexture extends DynamicTexture {
    private final ResourceLocation location;
    private final NativeImage backgroundBuffer;
    private final Object lock = new Object();
    private volatile boolean hasNewFrame = false;

    public VideoTexture(int width, int height) {
        super(width, height, true);
        this.backgroundBuffer = new NativeImage(width, height, false);
        this.location = new ResourceLocation("cobe_video", "video_" + UUID.randomUUID().toString().replace("-", "_"));
        Minecraft.getInstance().getTextureManager().register(this.location, this);
    }

    public void writePixels(java.nio.IntBuffer intBuf, int width, int height) {
        synchronized (lock) {
            for (int y = 0; y < height; y++) {
                int rowOffset = y * width;
                for (int x = 0; x < width; x++) {
                    int abgr = intBuf.get(rowOffset + x);

                    int destX = width - 1 - x;
                    backgroundBuffer.setPixelRGBA(destX, y, abgr);
                }
            }
            hasNewFrame = true;
        }
    }

    public void uploadFrame() {
        if (!hasNewFrame) return;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        synchronized (lock) {
            NativeImage internalPixels = this.getPixels();
            if (internalPixels != null) {
                internalPixels.copyFrom(backgroundBuffer);
                this.upload();
            }
            hasNewFrame = false;
        }
    }

    @Override
    public void close() {
        super.close();
        backgroundBuffer.close();
    }

    public ResourceLocation getLocation() {
        return this.location;
    }
}