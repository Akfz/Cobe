package n.paradox.cobe;

import n.paradox.aslib.annotation.DontCompile;
import n.paradox.aslib.datagen.fabric.mod.FabricModJsonData;
import n.paradox.aslib.datagen.fabric.mod.GenerateFabricModJson;
import n.paradox.aslib.datagen.forge.modstoml.GenerateModsToml;
import n.paradox.aslib.datagen.forge.modstoml.ModsTomlData;
import n.paradox.aslib.datagen.forge.packmcmeta.GeneratePackMcmeta;
import n.paradox.aslib.datagen.forge.packmcmeta.PackMcmetaData;

@DontCompile
public class DataGen {
    public static void main(String[] args) {
        new GenerateFabricModJson(new FabricModJsonData().mixin("cobe.mixins.json").entrypoint("n.paradox.cobe.CobeMod_fabric")).run("common");
        new GenerateModsToml(new ModsTomlData()).run("common");
        new GeneratePackMcmeta(new PackMcmetaData()).run("common");
    }
}
