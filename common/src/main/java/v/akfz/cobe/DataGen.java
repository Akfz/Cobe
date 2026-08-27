package v.akfz.cobe;

import v.akfz.aslib.datagen.fabric.mod.FabricModJsonData;
import v.akfz.aslib.datagen.fabric.mod.GenerateFabricModJson;
import v.akfz.aslib.datagen.forge.modstoml.GenerateModsToml;
import v.akfz.aslib.datagen.forge.modstoml.ModsTomlData;
import v.akfz.aslib.datagen.forge.packmcmeta.GeneratePackMcmeta;
import v.akfz.aslib.datagen.forge.packmcmeta.PackMcmetaData;
import v.akfz.db.annotation.DontCompile;

@DontCompile
public class DataGen {
    public static void main(String[] args) {
        new GenerateFabricModJson(new FabricModJsonData().mixin("cobe.mixins.json").entrypoint("v.akfz.cobe.CobeMod_fabric")
                .depend("aslib", ">=1.0")).run("common");
        new GenerateModsToml(new ModsTomlData().dependency("aslib", true, ">=1.0")).run("common");
        new GeneratePackMcmeta(new PackMcmetaData()).run("common");
        /*
        new GenerateLang().addLangs(
                new LangData("cobe", "ru_ru")
                        .add("warn.logo", "Внимание!")
                        .add("warn.text", "Для работы видео-текстур нужно скачать javacv, ffmpeg и их зависимости. Мы можем сделать это за вас, либо скачайте вручную и добавьте в minecraft/libs")
                        .add("warn.ok", "Хорошо, скачивай")
                        .add("warn.deny", "Отказано"),
                new LangData("cobe", "en_us")
                        .add("warn.logo", "Warning!")
                        .add("warn.text", "For video-textures to work, you need to download javacv, ffmpeg and their dependencies. We can download them for you, or download manually and put in minecraft/libs")
                        .add("warn.ok", "Ok, download it")
                        .add("warn.deny", "Deny")
        ).run("common");
         */
    }
}
