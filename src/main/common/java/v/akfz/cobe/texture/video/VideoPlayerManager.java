package v.akfz.cobe.texture.video;

import net.minecraft.resources.ResourceLocation;
import v.akfz.cobe.core.render.DefaultCobeRenderer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class VideoPlayerManager {
    private static final VideoPlayerManager INSTANCE = new VideoPlayerManager();
    public static VideoPlayerManager getInstance() { return INSTANCE; }

    private final Map<String, VideoStreamPlayer> activePlayers = new ConcurrentHashMap<>();

    private VideoPlayerManager() {}

    public ResourceLocation getOrCreatePlayer(File file, boolean loop, Runnable onFinished) {
        return obtain(keyOf(file), () -> new VideoStreamPlayer(file, loop, onFinished));
    }

    public ResourceLocation getOrCreatePlayer(ResourceLocation rl, boolean loop, Runnable onFinished) {
        return obtain(keyOf(rl), () -> new VideoStreamPlayer(rl, loop, onFinished));
    }

    public ResourceLocation getOrCreatePlayer(String relativePath, boolean loop, Runnable onFinished) {
        if (relativePath.contains(":")) {
            return getOrCreatePlayer(new ResourceLocation(relativePath), loop, onFinished);
        } else {
            return getOrCreatePlayer(new ResourceLocation("cobe", relativePath), loop, onFinished);
        }
    }

    private ResourceLocation obtain(String key, Supplier<VideoStreamPlayer> factory) {
        VideoStreamPlayer existing = activePlayers.get(key);
        if (existing != null) return existing.getTextureLocation();

        VideoStreamPlayer fresh = factory.get();
        VideoStreamPlayer prev = activePlayers.putIfAbsent(key, fresh);
        if (prev != null) {
            fresh.stop();
            return prev.getTextureLocation();
        }
        fresh.start();
        return fresh.getTextureLocation();
    }

    public void stopAndRelease(File file) {
        stopAndReleaseKey(keyOf(file));
    }

    public void stopAndRelease(ResourceLocation rl) {
        stopAndReleaseKey(keyOf(rl));
    }

    public void stopAndRelease(String relativePath) {
        if (relativePath.contains(":")) {
            stopAndRelease(new ResourceLocation(relativePath));
        } else {
            stopAndRelease(new ResourceLocation("cobe", relativePath));
        }
    }

    @Deprecated
    public void stopAndReleaseKey(String key) {
        VideoStreamPlayer player = activePlayers.remove(key);
        if (player != null) player.stop();
    }

    private static String keyOf(File f)              { return "file_" + f.getAbsolutePath(); }
    private static String keyOf(ResourceLocation rl) { return "rl_"   + rl.toString(); }

    public void stopAll() {
        Map<String, VideoStreamPlayer> snapshot;
        synchronized (this) {
            snapshot = new HashMap<>(activePlayers);
            activePlayers.clear();
        }
        snapshot.values().forEach(VideoStreamPlayer::stop);
    }
}