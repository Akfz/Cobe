package v.akfz.cobe;

import v.akfz.aslib.datagen.fabric.mod.FabricModJsonData;
import v.akfz.aslib.datagen.fabric.mod.GenerateFabricModJson;
import v.akfz.aslib.datagen.forge.modstoml.GenerateModsToml;
import v.akfz.aslib.datagen.forge.modstoml.ModsTomlData;
import v.akfz.aslib.datagen.forge.packmcmeta.GeneratePackMcmeta;
import v.akfz.aslib.datagen.forge.packmcmeta.PackMcmetaData;
import v.akfz.db.annotation.DevOnly;

@DevOnly
public class DataGen {
    public static void main(String[] args) {
        new GenerateFabricModJson(new FabricModJsonData().mixin("cobe.mixins.json").entrypoint("v.akfz.cobe.CobeMod_fabric")
                .depend("aslib", ">=1.0")).run("");
        new GenerateModsToml(new ModsTomlData().dependency("aslib", true, ">=1.0")).run("");
        new GeneratePackMcmeta(new PackMcmetaData()).run("");
    }
}
