package v.akfz.cobe.core.cache;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.data.loader.json.model.ModelData;

import java.util.HashMap;
import java.util.Map;

public class ModelCache {

    private static volatile Map<String, ModelData> CACHED_MODEL = Map.of();

    public static synchronized void addCacheModel(ModelData model, String name) {
        if (model == null || name == null) {
            return;
        }

        Map<String, ModelData> newMap = new HashMap<>(CACHED_MODEL);
        newMap.put(name, model);
        CACHED_MODEL = Map.copyOf(newMap);
    }

    @Nullable
    public static ModelData getFromCache(String name) {
        if (name == null) {
            return null;
        }
        return CACHED_MODEL.get(name);
    }

    public static Map<String, ModelData> getFromCache() {
        return new HashMap<>(CACHED_MODEL);
    }
}