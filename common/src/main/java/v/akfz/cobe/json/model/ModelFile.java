package v.akfz.cobe.json.model;

import n.paradox.aslib.util.json.JsonFile;

import java.nio.file.Path;

public class ModelFile implements JsonFile<ModelData> {
    @Override
    public ModelData data() {
        return new ModelData();
    }

    //только чтение
    @Override
    public Path getPath() {
        return null;
    }
}
