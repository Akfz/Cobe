package v.akfz.cobe;

import v.akfz.aslib.AsLib;
import v.akfz.aslib.initializer.generator.GenerateInitializer;
import v.akfz.aslib.initializer.generator.InitializerClass;
import v.akfz.aslib.initializer.generator.LoaderType;
import v.akfz.aslib.resourcepack.configpack.ConfigPack;
import v.akfz.aslib.resourcepack.configpack.ConfigPackRegistry;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.cobe.event.listener.RegisterEntityRendererListener;
import v.akfz.cobe.loader.resourcepack.configpack.CobeCFGPack;

@GenerateInitializer(loader = LoaderType.Both, modId = "cobe")
public class CobeMod implements InitializerClass {
    @Override
    public void init() {
        if (GlobalUtils.isClientSide()) {
            ConfigPackRegistry.register("cobe", (path, data) -> new CobeCFGPack(data.name, path, data.id));
            AsLib.EVENT_BUS.register(new RegisterEntityRendererListener());
            ConfigPack.Init();
        }
    }
}