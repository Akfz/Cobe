package v.akfz.cobe.loader.resourcepack.configpack;

import v.akfz.aslib.resourcepack.SimpleFileResourcePack;
import v.akfz.cobe.loader.util.FileLoader;

import java.nio.file.Path;

public class ConfigResourcePack extends SimpleFileResourcePack {

    public ConfigResourcePack(String packName, Path root, String namespace) {
        super(packName, root, namespace);
        this.initializeResources();
    }

    private void initializeResources() {
        this.getCache().forEach((relativePath, path) -> {
            if (relativePath.toLowerCase().endsWith(".json")) {
                FileLoader.FileType type = FileLoader.identifyType(path);

                if (type == FileLoader.FileType.MODEL) {
                    FileLoader.loadModelFile(path);
                } else if (type == FileLoader.FileType.ANIMATION) {
                    FileLoader.loadAnimationFile(path);
                }
            }
        });
    }

    @Override
    public void refreshCache() {
        super.refreshCache();
        this.initializeResources();
    }
}