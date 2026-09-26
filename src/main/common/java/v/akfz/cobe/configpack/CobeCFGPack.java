package v.akfz.cobe.configpack;

import org.jetbrains.annotations.Nullable;
import v.akfz.aslib.resourcepack.SimpleFileResourcePack;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.cobe.core.math.loader.FileLoader;

import java.nio.file.Path;

public class CobeCFGPack extends SimpleFileResourcePack {

    public CobeCFGPack(String packName, Path root, String namespace) {
        super(packName, root, namespace);
        this.initializeResources();
    }

    private void initializeResources() {
        this.getCache().forEach((relativePath, path) -> {
            String lowerPath = relativePath.toLowerCase();
            if (lowerPath.endsWith(".json") || lowerPath.endsWith(".hb")) {
                FileLoader.FileType type = FileLoader.identifyType(path);
                if (type == null) {
                    failToLoad(null, path);
                    return;
                }
                switch (type) {
                    case ANIMATION -> FileLoader.loadAnimationFile(path);
                    case HITBOX -> FileLoader.loadHitboxFile(path);
                    case MODEL -> FileLoader.loadModelFile(path);
                    case UNKNOWN -> {
                        failToLoad(type,path);
                    }
                }
            }
        });
    }

    private void failToLoad(@Nullable FileLoader.FileType type, Path path) {
        StringBuilder log = new StringBuilder("Fail to load : ");
        if (type != null) {
            log.append("File type - ").append(type.name());
        }
        log.append("path - ").append(path);
        System.out.println(log);
    }

    @Override
    public void refreshCache() {
        super.refreshCache();
        this.initializeResources();
    }
}