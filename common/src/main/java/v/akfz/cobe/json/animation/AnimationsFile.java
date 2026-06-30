package v.akfz.cobe.json.animation;

import n.paradox.aslib.util.json.JsonFile;

import java.nio.file.Path;

public class AnimationsFile implements JsonFile<AnimationsData> {
    @Override
    public AnimationsData data() {
        return new AnimationsData();
    }

    //только чтение
    @Override
    public Path getPath() {
        return null;
    }
}
