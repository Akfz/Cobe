package v.akfz.cobe.core.math.loader;

import com.google.gson.stream.JsonReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.cobe.core.cache.AnimationCache;
import v.akfz.cobe.core.cache.HitboxCache;
import v.akfz.cobe.core.cache.ModelCache;
import v.akfz.cobe.core.data.Animation;
import v.akfz.cobe.core.data.hitbox.HitboxData;
import v.akfz.cobe.core.data.loader.json.animation.AnimationsData;
import v.akfz.cobe.core.data.loader.json.model.ModelData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileLoader {
    private FileLoader() {}

    public static void loadAnimationFile(Path path) {
        AnimationsData data = GsonHelper.read(path, AnimationsData.class);
        if (data == null || data.animations == null) return;

        for (Animation anim : data.animations) {
            AnimationCache.addCacheAnimation(anim, anim.name());
        }
        AnimationCache.addCacheAnimationData(data, path.toString());
    }

    public static void loadHitboxFile(Path path) {
        HitboxData data = GsonHelper.read(path, HitboxData.class);
        if (data == null) return;

        String fileName = path.getFileName().toString();
        HitboxCache.addCacheHitbox(data, fileName);
    }
    //
    public static void loadModelFile(ResourceLocation location, ResourceManager manager) {
        ModelData data = GsonHelper.read(location, ModelData.class, manager);
        if (data == null || data.nameOfModel == null) return;

        ModelCache.addCacheModel(data, data.nameOfModel);
    }

    public static void loadModelFile(Path path) {
        ModelData data = GsonHelper.read(path, ModelData.class);
        if (data == null || data.nameOfModel == null) return;

        ModelCache.addCacheModel(data, data.nameOfModel);
    }

    public static void loadAnimationFile(ResourceLocation location, ResourceManager manager) {
        AnimationsData data = GsonHelper.read(location, AnimationsData.class, manager);
        if (data == null || data.animations == null) return;

        for (Animation anim : data.animations) {
            AnimationCache.addCacheAnimation(anim, anim.name());
        }
        AnimationCache.addCacheAnimationData(data, location.toString());
    }

    public static void loadHitboxFile(ResourceLocation location, ResourceManager manager) {
        HitboxData data = GsonHelper.read(location, HitboxData.class, manager);
        if (data == null) return;

        String path = location.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        HitboxCache.addCacheHitbox(data, fileName);
    }

    @Nullable
    public static FileType identifyType(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return identifyType(reader);
        } catch (IOException e) {
            System.err.println("Failed to read file from path: " + path);
        }
        return null;
    }

    public static FileType identifyType(ResourceLocation location, ResourceManager manager) {
        try (var is = manager.getResource(location).isPresent() ? manager.getResource(location).get().open() : null;
             BufferedReader reader = is != null ? new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)) : null) {
            if (reader == null) return FileType.UNKNOWN;
            return identifyType(reader);
        } catch (IOException e) {
            System.err.println("Failed to read resource: " + location);
        }
        return FileType.UNKNOWN;
    }

    private static FileType identifyType(java.io.Reader reader) {
        try (JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if ("hbVersion".equals(name)) return FileType.HITBOX;
                if ("texturePaths".equals(name) || "bones".equals(name)) return FileType.MODEL;
                if ("animations".equals(name)) return FileType.ANIMATION;
                jsonReader.skipValue();
            }
            jsonReader.endObject();
        } catch (Exception ignored) {}
        return FileType.UNKNOWN;
    }

    public enum FileType {
        MODEL, ANIMATION, HITBOX, UNKNOWN
    }
}