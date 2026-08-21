package v.akfz.cobe.aengine.data.cache;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.loader.json.model.ModelData;

import java.util.HashMap;
import java.util.Map;

//тут просто все загруженные модели, берутся по их имени (instance-like будет сильно жрать ресурсы!)
public class ModelCache {
    private static final Map<String, ModelData> CACHED_MODEL = new HashMap<>();
    public static void addCacheModel(ModelData a, String name) {
        CACHED_MODEL.put(name,a);
    }
    @Nullable
    public static ModelData getFromCache(String name) {
        return CACHED_MODEL.get(name);
    }
    public static Map<String, ModelData> getFromCache() {
        return new HashMap<>(CACHED_MODEL);
    }
}
