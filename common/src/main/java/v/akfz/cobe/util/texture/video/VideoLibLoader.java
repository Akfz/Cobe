package v.akfz.cobe.util.texture.video;

import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class VideoLibLoader {
    private static ClassLoader videoClassLoader;
    private static boolean isLoaded = false;
    private static boolean isFailed = false;

    private static final String JAVACV_VER = "1.5.10";
    private static final String FFMPEG_VER = "6.1.1-1.5.10";

    public static boolean isAvailable() {
        return isLoaded;
    }

    public static synchronized boolean ensureLoaded() {
        if (isLoaded) return true;
        if (isFailed) return false;

        try {
            Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
            videoClassLoader = VideoLibLoader.class.getClassLoader();
            isLoaded = true;
            return true;
        } catch (ClassNotFoundException ignored) {}

        try {
            Path libDir = Minecraft.getInstance().gameDirectory.toPath().resolve("cobe_libs");
            Files.createDirectories(libDir);

            String platform = detectPlatform();
            if (platform == null) {
                isFailed = true;
                return false;
            }

            List<URL> urls = new ArrayList<>();
            downloadAndAdd(libDir, urls, "org/bytedeco", "javacv", JAVACV_VER, null);
            downloadAndAdd(libDir, urls, "org/bytedeco", "javacpp", JAVACV_VER, null);
            downloadAndAdd(libDir, urls, "org/bytedeco", "javacpp", JAVACV_VER, platform);
            downloadAndAdd(libDir, urls, "org/bytedeco", "ffmpeg", FFMPEG_VER, null);
            downloadAndAdd(libDir, urls, "org/bytedeco", "ffmpeg", FFMPEG_VER, platform);

            videoClassLoader = new URLClassLoader(urls.toArray(new URL[0]), VideoLibLoader.class.getClassLoader());

            Class<?> loaderClass = Class.forName("org.bytedeco.javacpp.Loader", true, videoClassLoader);
            Class<?> avutilClass = Class.forName("org.bytedeco.ffmpeg.global.avutil", true, videoClassLoader);
            loaderClass.getMethod("load", Class.class).invoke(null, avutilClass);

            isLoaded = true;
            return true;
        } catch (Exception e) {
            System.err.println("[Cobe Video] Не удалось загрузить FFmpeg библиотеки: " + e.getMessage());
            isFailed = true;
            return false;
        }
    }

    public static ClassLoader getClassLoader() {
        return videoClassLoader;
    }

    private static void downloadAndAdd(Path dir, List<URL> urls, String group, String artifact, String ver, String classifier) throws Exception {
        String jarName = artifact + "-" + ver + (classifier != null ? "-" + classifier : "") + ".jar";
        Path target = dir.resolve(jarName);

        if (!Files.exists(target)) {
            String urlStr = String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s",
                    group, artifact, ver, jarName);
            System.out.println("[Cobe Video] Скачивание библиотеки: " + jarName);
            try (InputStream in = new URL(urlStr).openStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        urls.add(target.toUri().toURL());
    }

    private static String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        boolean isArm = arch.contains("arm") || arch.contains("aarch64");

        if (os.contains("win")) return "windows-x86_64";
        if (os.contains("mac")) return isArm ? "macosx-arm64" : "macosx-x86_64";
        if (os.contains("nix") || os.contains("nux")) return isArm ? "linux-arm64" : "linux-x86_64";
        return null;
    }
}