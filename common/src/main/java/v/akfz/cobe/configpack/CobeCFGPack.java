package v.akfz.cobe.configpack;

import v.akfz.aslib.resourcepack.SimpleFileResourcePack;
import v.akfz.cobe.core.math.loader.FileLoader;

import java.nio.file.Path;

public class CobeCFGPack extends SimpleFileResourcePack {

    public CobeCFGPack(String packName, Path root, String namespace) {
        super(packName, root, namespace);
        this.initializeResources();
    }

    private void initializeResources() {
        this.getCache().forEach((relativePath, path) -> {
            if (relativePath.toLowerCase().endsWith(".json") || relativePath.toLowerCase().endsWith(".hb") ) {
                FileLoader.FileType type = FileLoader.identifyType(path);

                switch (type) {
	                case MODEL -> {
                        FileLoader.loadModelFile(path);
                    }
	                case ANIMATION -> {
                        FileLoader.loadAnimationFile(path);
                    }
	                case HITBOX ->{
                        FileLoader.loadHitboxFile(path);
                    }
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