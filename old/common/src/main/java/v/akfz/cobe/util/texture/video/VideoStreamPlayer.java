package v.akfz.cobe.util.texture.video;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import v.akfz.cobe.render.CobeRenderer;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class VideoStreamPlayer {
    private final File videoFile;
    private final ResourceLocation resourceLocation;
    private final boolean loop;
    private final Runnable onFinished;
    private VideoTexture videoTexture;

    private Thread videoDecodingThread;
    private volatile boolean isPlaying = false;

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

    public void start() {
        if (isPlaying) return;
        this.isPlaying = true;

        this.videoDecodingThread = new Thread(() -> {
            FFmpegFrameGrabber grabber = null;
            try {
                while (isPlaying) {
                    if (videoFile != null) {
                        if (!videoFile.exists()) {
                            System.err.println("[Cobe Video] Видеофайл не найден: " + videoFile.getAbsolutePath());
                            return;
                        }
                        grabber = new FFmpegFrameGrabber(videoFile.getAbsolutePath());
                    } else if (resourceLocation != null) {
                        Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
                        if (resourceOpt.isEmpty()) {
                            System.err.println("[Cobe Video] Ресурс не найден: " + resourceLocation);
                            return;
                        }
                        InputStream inputStream = resourceOpt.get().open();
                        grabber = new FFmpegFrameGrabber(inputStream);
                    } else {
                        return;
                    }

                    grabber.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA);
                    grabber.start();

                    int videoWidth = grabber.getImageWidth();
                    int videoHeight = grabber.getImageHeight();
                    double videoFps = grabber.getVideoFrameRate();
                    if (videoFps <= 0.0) videoFps = 30.0;

                    long frameDelayMs = (long) (1000.0 / videoFps);

                    if (this.videoTexture == null) {
                        CompletableFuture<Void> initFuture = new CompletableFuture<>();
                        Minecraft.getInstance().execute(() -> {
                            this.videoTexture = new VideoTexture(videoWidth, videoHeight);
                            initFuture.complete(null);
                        });
                        initFuture.join();
                    }

                    Frame frame;
                    while (isPlaying && (frame = grabber.grabImage()) != null) {
                        long startTime = System.currentTimeMillis();

                        if (frame.image != null && frame.image.length > 0) {
                            ByteBuffer byteBuf = (ByteBuffer) frame.image[0];
                            IntBuffer intBuf = byteBuf.asIntBuffer();

                            this.videoTexture.writePixels(intBuf, videoWidth, videoHeight);
                        }

                        long elapsed = System.currentTimeMillis() - startTime;
                        long sleepTime = frameDelayMs - elapsed;
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                        }
                    }

                    grabber.stop();
                    grabber.release();
                    grabber = null;

                    if (!loop) {
                        this.isPlaying = false;
                        if (onFinished != null) {
                            onFinished.run();
                        }
                        break;
                    }
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                System.err.println("[Cobe Video] Ошибка декодера FFmpeg: " + e.getMessage());
            } finally {
                if (grabber != null) {
                    try {
                        grabber.stop();
                        grabber.release();
                    } catch (Exception ignored) {}
                }
            }
        }, "Cobe-Video-Streaming-" + (videoFile != null ? videoFile.getName() : resourceLocation));

        this.videoDecodingThread.setDaemon(true);
        this.videoDecodingThread.start();
    }

    public void stop() {
        this.isPlaying = false;
        if (this.videoDecodingThread != null) {
            this.videoDecodingThread.interrupt();
        }

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (this.videoTexture != null) {
                mc.getTextureManager().release(this.videoTexture.getLocation());
                this.videoTexture.close();
            }
        });
    }

    public ResourceLocation getTextureLocation() {
        if (this.videoTexture == null) {
            return CobeRenderer.NULL_TEXTURE;
        }
        this.videoTexture.uploadFrame();
        return this.videoTexture.getLocation();
    }
}