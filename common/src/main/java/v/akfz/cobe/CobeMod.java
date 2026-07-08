package v.akfz.cobe;

import v.akfz.aslib.initializer.generator.GenerateInitializer;
import v.akfz.aslib.initializer.generator.InitializerClass;
import v.akfz.aslib.initializer.generator.LoaderType;
import v.akfz.aslib.resourcepack.AsLibResourceResourceReloaderHelper;
import v.akfz.cobe.loader.resourcepack.configpack.ConfigPack;
import v.akfz.cobe.loader.util.FileLoader;

@GenerateInitializer(loader = LoaderType.Both, modId = "cobe")
public class CobeMod implements InitializerClass {
    @Override
    public void init() {
        AsLibResourceResourceReloaderHelper.register("loadCobe", (manager) -> {
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