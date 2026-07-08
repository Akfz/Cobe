package v.akfz.cobe;

import v.akfz.aslib.AsLib;
import v.akfz.aslib.initializer.generator.GenerateInitializer;
import v.akfz.aslib.initializer.generator.InitializerClass;
import v.akfz.aslib.initializer.generator.LoaderType;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.cobe.loader.resourcepack.configpack.ConfigPack;
import v.akfz.cobe.test.TestEntityRenderRegister;

@GenerateInitializer(loader = LoaderType.Both, modId = "cobe")
public class CobeMod implements InitializerClass {
    @Override
    public void init() {
        if (GlobalUtils.isClientSide()) {
            AsLib.EVENT_BUS.register(new TestEntityRenderRegister());

            ConfigPack.Init();
        }
    }
}