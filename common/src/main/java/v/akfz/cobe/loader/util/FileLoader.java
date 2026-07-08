package v.akfz.cobe.loader.util;

import com.google.gson.stream.JsonReader;
import v.akfz.aslib.util.json.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.aengine.data.cache.AnimationCache;
import v.akfz.cobe.aengine.data.cache.ModelCache;
import v.akfz.cobe.loader.json.animation.AnimationsData;
import v.akfz.cobe.loader.json.model.ModelData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileLoader {
    private FileLoader(){}
    public static void loadAnimationFile(ResourceLocation location) {
        AnimationsData data = GsonHelper.read(location, AnimationsData.class);
        if (data == null) {
            System.out.println("Failed to load animation from RL path : " + location.toString());
            return;
        }
        AnimationCache.addCacheAnimationData(data,location.toString());
    }

    public static void loadAnimationFile(Path path) {
        AnimationsData data = GsonHelper.read(path, AnimationsData.class);
        if (data == null) {
            System.out.println("Failed to load animation from path : " + path.toString());
            return;
        }
        AnimationCache.addCacheAnimationData(data,path.toString());
    }

    public static void loadModelFile(ResourceLocation location) {
        ModelData data = GsonHelper.read(location, ModelData.class);
        if (data == null) {
            System.out.println("Failed to load model from path : " + location.toString());
            return;
        }
        ModelCache.addCacheModel(data,data.nameOfModel);
    }

    public static void loadModelFile(Path path) {
        ModelData data = GsonHelper.read(path, ModelData.class);
        if (data == null) {
            System.out.println("Failed to load model from path : " + path.toString());
            return;
        }
        ModelCache.addCacheModel(data,data.nameOfModel);
    }

    public enum FileType {
        MODEL,
        ANIMATION,
        UNKNOWN
    }

    public static FileType identifyType(ResourceLocation location, ResourceManager manager) {
        try (InputStream is = manager.getResourceOrThrow(location).open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return identifyType(reader);
        } catch (IOException e) {
            System.err.println("Failed to read resource: " + location);
        }
        return FileType.UNKNOWN;
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

    private static FileType identifyType(Reader reader) {
        try (JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();

                if ("texturePaths".equals(name) || "bones".equals(name)) {
                    return FileType.MODEL;
                } else if ("animations".equals(name)) {
                    return FileType.ANIMATION;
                }

                jsonReader.skipValue();
            }
            jsonReader.endObject();
        } catch (Exception ignored) {
        }
        return FileType.UNKNOWN;
    }
}
