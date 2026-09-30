package v.akfz.cobe;

import v.akfz.aslib.AsLib;
import v.akfz.aslib.resourcepack.ModAssetsRegistrar;
import v.akfz.aslib.resourcepack.configpack.ConfigPack;
import v.akfz.aslib.resourcepack.configpack.ConfigPackRegistry;
import v.akfz.cobe.configpack.CobeCFGPack;
import v.akfz.cobe.event.listener.TickListener;
import v.akfz.cobe.nat.NativeLoader;
import v.akfz.db.generator.GenerateInitializer;

@GenerateInitializer(modId = "cobe")
public final class CobeMod {
    public void init() {
        NativeLoader.load("cobe_native_decoder");
        AsLib.EVENT_BUS.register(new TickListener());
        ConfigPackRegistry.register("cobe", (path, data) -> new CobeCFGPack(data.name, path, data.id));
        ConfigPack.Init();

        ModAssetsRegistrar.registerModAssets("cobe");
    }
}