package v.akfz.cobe;

import v.akfz.aslib.AsLib;
import v.akfz.aslib.resourcepack.ModAssetsRegistrar;
import v.akfz.aslib.resourcepack.configpack.ConfigPack;
import v.akfz.aslib.resourcepack.configpack.ConfigPackRegistry;
import v.akfz.cobe.configpack.CobeCFGPack;
import v.akfz.cobe.event.listener.TickListener;
import v.akfz.db.generator.GenerateInitializer;

@GenerateInitializer(modId = "cobe")
public class CobeMod {
    public void init() {
        AsLib.EVENT_BUS.register(new TickListener());
        ConfigPackRegistry.register("cobe", (path, data) -> new CobeCFGPack(data.name, path, data.id));
        //AsLib.EVENT_BUS.register(new RegisterEntityRendererListenerTEST());
        ConfigPack.Init();

        ModAssetsRegistrar.registerModAssets("cobe");
    }
}