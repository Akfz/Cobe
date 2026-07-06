package n.paradox.cobe;

import n.paradox.aslib.initializer.generator.GenerateInitializer;
import n.paradox.aslib.initializer.generator.InitializerClass;
import n.paradox.aslib.initializer.generator.LoaderType;
import n.paradox.aslib.resourcepack.AsLibResourceResourceReloaderHelper;
import v.akfz.cobe.aengine.data.cache.AnimationCache;
import v.akfz.cobe.aengine.data.cache.ModelCache;
import v.akfz.cobe.loader.json.animation.Animation;
import v.akfz.cobe.loader.resourcepack.configpack.ConfigPack;
import v.akfz.cobe.loader.util.FileLoader;

@GenerateInitializer(loader = LoaderType.Both, modId = "cobe")
public class CobeMod implements InitializerClass {
    @Override
    public void init() {
        AsLibResourceResourceReloaderHelper.register("loadAnimations", (manager) -> {
            var allResources = manager.listResources("",
                    location -> location.getPath().endsWith(".json")
            );

            allResources.forEach((location, resource) -> {
                if (FileLoader.identifyType(location, manager) == FileLoader.FileType.ANIMATION) {
                    FileLoader.loadAnimationFile(location);
                } else if (FileLoader.identifyType(location, manager) == FileLoader.FileType.MODEL){
                    FileLoader.loadModelFile(location);
                }
            });
        });

        ConfigPack.Init();
    }
}