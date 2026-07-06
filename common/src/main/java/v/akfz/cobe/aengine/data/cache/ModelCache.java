package v.akfz.cobe.aengine.data.cache;

import v.akfz.cobe.loader.json.model.ModelData;

import java.util.HashMap;
import java.util.Map;

//тут просто все загруженные модели, берутся по их имени (instance-like будет сильно жрать ресурсы!)
public class ModelCache {
    public static final Map<String, ModelData> CACHED_MODEL = new HashMap<>();
}
