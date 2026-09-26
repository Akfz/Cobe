package v.akfz.cobe.texture.video;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import v.akfz.cobe.core.render.DefaultCobeRenderer;
import v.akfz.cobe.nat.NativeVideoDecoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class VideoStreamPlayer {

    private static final double MAX_FPS = 60.0;
    private static final long MIN_FRAME_DELAY_MS = 1L;

    private final File videoFile;
    private final ResourceLocation resourceLocation;
    private final boolean loop;
    private final Runnable onFinished;

    private VideoTexture videoTexture;
    private Thread videoDecodingThread;

    private volatile boolean isPlaying = false;
    private volatile boolean isPaused  = false;
    private volatile boolean stopped   = false;

    public VideoStreamPlayer(File videoFile, boolean loop, Runnable onFinished) {
        this.videoFile = videoFile;
        this.resourceLocation = null;
        this.loop = loop;
        this.onFinished = onFinished;
    }

    public VideoStreamPlayer(ResourceLocation resourceLocation, boolean loop, Runnable onFinished) {
        this.videoFile = null;
        this.resourceLocation = resourceLocation;
        this.loop = loop;
        this.onFinished = onFinished;
    }

    public synchronized void start() {
        if (isPlaying) return;
        this.isPlaying = true;
        this.stopped = false;

        this.videoDecodingThread = new Thread(this::decodeLoop,
                "Cobe-Video-" + (videoFile != null ? videoFile.getName() : String.valueOf(resourceLocation)));
        this.videoDecodingThread.setDaemon(true);
        this.videoDecodingThread.start();
    }

    public synchronized void stop() {
        if (stopped) return;
        this.stopped = true;
        this.isPlaying = false;

        Thread t = this.videoDecodingThread;
        this.videoDecodingThread = null;
        if (t != null) t.interrupt();

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (this.videoTexture != null) {
                mc.getTextureManager().release(this.videoTexture.getLocation());
                this.videoTexture.close();
                this.videoTexture = null;
            }
        });
    }

    public void pause()  { isPaused = true; }
    public void resume() { isPaused = false; }
    public boolean isPlaying() { return isPlaying; }
    public boolean isPaused()  { return isPaused; }

    public ResourceLocation getTextureLocation() {
        VideoTexture tex = this.videoTexture;
        if (tex == null) return DefaultCobeRenderer.NULL_TEXTURE;
        tex.uploadFrame();
        return tex.getLocation();
    }

    private void decodeLoop() {
        NativeVideoDecoder decoder = null;
        try {
            byte[] fileData = readAllBytes();
            if (fileData == null) return;

            try {
                decoder = new NativeVideoDecoder(fileData);
            } catch (IllegalArgumentException e) {
                System.err.println("[Cobe] Cannot decode video: " + e.getMessage());
                return;
            }

            final int w = decoder.getWidth();
            final int h = decoder.getHeight();
            if (w <= 0 || h <= 0) {
                System.err.println("[Cobe] Invalid video dimensions " + w + "x" + h);
                return;
            }

            double srcFps = decoder.getFps();
            if (srcFps <= 0.0) srcFps = 30.0;
            double playFps = Math.min(srcFps, MAX_FPS);
            long frameDelayMs = Math.max(MIN_FRAME_DELAY_MS, (long)(1000.0 / playFps));
            final int knownFrameCount = decoder.getFrameCount();

            if (this.videoTexture == null) {
                CompletableFuture<Void> initFuture = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> {
                    try {
                        this.videoTexture = new VideoTexture(w, h);
                        initFuture.complete(null);
                    } catch (Throwable t) {
                        initFuture.completeExceptionally(t);
                    }
                });
                try {
                    initFuture.join();
                } catch (Exception e) {
                    System.err.println("[Cobe] Cannot create VideoTexture: " + e);
                    return;
                }
            }

            ByteBuffer rgba = decoder.frameBuffer();
            IntBuffer intBuf = rgba.asIntBuffer();

            int index = 0;

            while (isPlaying && !stopped) {
                if (isPaused) {
                    Thread.sleep(30);
                    continue;
                }

                long startNs = System.nanoTime();

                boolean hasFrame = decoder.decodeFrame(index);

                if (!hasFrame) {
                    if (loop) {
                        index = 0;
                        continue;
                    }
                    break;
                }

                VideoTexture tex = this.videoTexture;
                if (tex == null) break;

                intBuf.rewind();
                tex.writePixels(intBuf, w, h);

                index++;
                if (knownFrameCount > 0 && index >= knownFrameCount) {
                    if (!loop) break;
                    index = 0;
                }

                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                long sleep = frameDelayMs - elapsedMs;
                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            }

        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            System.err.println("[Cobe] Video decode error: " + t);
            t.printStackTrace();
        } finally {
            if (decoder != null) {
                try { decoder.close(); } catch (Throwable ignored) {}
            }
            isPlaying = false;

            if (!loop && onFinished != null) {
                Minecraft.getInstance().execute(onFinished);
            }
        }
    }

    private byte[] readAllBytes() throws Exception {
        if (videoFile != null) {
            if (!videoFile.exists()) return null;
            try (InputStream in = new FileInputStream(videoFile)) {
                return readStream(in);
            }
        }
        if (resourceLocation != null) {
            Optional<Resource> opt = Minecraft.getInstance()
                    .getResourceManager().getResource(resourceLocation);
            if (opt.isEmpty()) return null;
            try (InputStream in = opt.get().open()) {
                return readStream(in);
            }
        }
        return null;
    }

    private static byte[] readStream(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 20);
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }
}