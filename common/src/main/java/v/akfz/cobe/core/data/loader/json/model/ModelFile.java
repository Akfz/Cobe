package v.akfz.cobe.core.data.loader.json.model;

import v.akfz.aslib.util.json.JsonFile;

import java.nio.file.Path;

public class ModelFile implements JsonFile<ModelData> {
    @Override
    public ModelData data() {
        return new ModelData();
    }

    @Override
    public Path getPath() {
        return null;
    }
}
