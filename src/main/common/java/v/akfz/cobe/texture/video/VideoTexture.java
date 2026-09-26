package v.akfz.cobe.texture.video;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.nio.IntBuffer;
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

    public void writePixels(IntBuffer intBuf,int width,int height) {
        int[] row = new int[width];
        synchronized (lock) {
            intBuf.rewind();
            for (int y = 0; y < height; y++) {
                intBuf.get(row, 0, width);
                for (int x = 0; x < width; x++) {
                    backgroundBuffer.setPixelRGBA(x, y, row[x]);
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
            if (!hasNewFrame) return;
            NativeImage internalPixels = this.getPixels();
            if (internalPixels != null) {
                internalPixels.copyFrom(backgroundBuffer);
            }
            hasNewFrame = false;
        }
        this.upload();
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