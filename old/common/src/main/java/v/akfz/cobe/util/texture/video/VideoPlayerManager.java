package v.akfz.cobe.util.texture.video;

import net.minecraft.resources.ResourceLocation;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VideoPlayerManager {
    private static final VideoPlayerManager INSTANCE = new VideoPlayerManager();
    public static VideoPlayerManager getInstance() { return INSTANCE; }

    static {
        try {
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
        } catch (Throwable e) {
            System.err.println("[Cobe Video] Failed to preload FFmpeg libraries: " + e.getMessage());
        }
    }

    private final Map<String, VideoStreamPlayer> activePlayers = new ConcurrentHashMap<>();

    private VideoPlayerManager() {}

    public ResourceLocation getOrCreatePlayer(File file, boolean loop, Runnable onFinished) {
        String key = "file_" + file.getAbsolutePath();
        VideoStreamPlayer player = activePlayers.computeIfAbsent(key, path -> {
            VideoStreamPlayer p = new VideoStreamPlayer(file, loop, onFinished);
            p.start();
            return p;
        });
        return player.getTextureLocation();
    }

    public ResourceLocation getOrCreatePlayer(ResourceLocation resourceLocation, boolean loop, Runnable onFinished) {
        String key = "rl_" + resourceLocation.toString();
        VideoStreamPlayer player = activePlayers.computeIfAbsent(key, path -> {
            VideoStreamPlayer p = new VideoStreamPlayer(resourceLocation, loop, onFinished);
            p.start();
            return p;
        });
        return player.getTextureLocation();
    }

    public ResourceLocation getOrCreatePlayer(String relativePath, boolean loop, Runnable onFinished) {
        if (relativePath.contains(":")) {
            return getOrCreatePlayer(new ResourceLocation(relativePath), loop, onFinished);
        } else {
            return getOrCreatePlayer(new ResourceLocation("cobe", relativePath), loop, onFinished);
        }
    }

    public void stopAndRelease(String key) {
        VideoStreamPlayer player = activePlayers.remove(key);
        if (player != null) {
            player.stop();
        }
    }

    public void stopAll() {
        for (String key : activePlayers.keySet()) {
            stopAndRelease(key);
        }
    }
}