package v.akfz.cobe.texture.video;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryUtil;
import v.akfz.cobe.mixinterface.NativeImageAccessor;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;

public class VideoTexture extends DynamicTexture {
    private final ResourceLocation location;
    private final NativeImage backgroundBuffer;
    private final Object lock = new Object();
    private volatile boolean hasNewFrame = false;
    private final long bufferAddress;

    public VideoTexture(int width, int height) {
        super(width, height, true);
        this.backgroundBuffer = new NativeImage(width, height, false);
        this.bufferAddress = ((NativeImageAccessor) (Object) this.backgroundBuffer).getPixels();
        this.location = new ResourceLocation("cobe_video", "video_" + UUID.randomUUID().toString().replace("-", "_"));
        Minecraft.getInstance().getTextureManager().register(this.location, this);
    }

    public void writePixelsDirect(ByteBuffer directBuffer, int width, int height) {
        synchronized (lock) {
            long bytesToCopy = (long) width * height * 4L;
            long srcAddress = MemoryUtil.memAddress(directBuffer);
            if (srcAddress != 0 && bufferAddress != 0) {
                MemoryUtil.memCopy(srcAddress, bufferAddress, bytesToCopy);
                hasNewFrame = true;
            }
        }
    }

    public void writePixels(IntBuffer intBuf, int width, int height) {
        synchronized (lock) {
            long bytesToCopy = (long) width * height * 4L;
            long srcAddress = MemoryUtil.memAddress(intBuf);
            if (srcAddress != 0 && bufferAddress != 0) {
                MemoryUtil.memCopy(srcAddress, bufferAddress, bytesToCopy);
                hasNewFrame = true;
            }
        }
    }

    public void uploadFrame() {
        if (!hasNewFrame) return;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        synchronized (lock) {
            if (!hasNewFrame) return;
            NativeImage internalPixels = this.getPixels();
            if (internalPixels != null && bufferAddress != 0) {
                long ptr = ((NativeImageAccessor) (Object) internalPixels).getPixels();
                if (ptr != 0) {
                    long bytesToCopy = (long) backgroundBuffer.getWidth() * backgroundBuffer.getHeight() * 4L;
                    MemoryUtil.memCopy(bufferAddress, ptr, bytesToCopy);
                }
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