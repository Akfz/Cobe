package v.akfz.cobe.loader.resourcepack.configpack;

import v.akfz.aslib.resourcepack.AddResourcePack;
import v.akfz.aslib.resourcepack.SimpleFileResourcePack;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.aslib.util.json.JsonFile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;
import v.akfz.cobe.loader.resourcepack.configpack.preview.PreviewConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

//Создает ресурспаки на основе конфигов
public class ConfigPack {
    private ConfigPack(){}
    private static boolean isLoaded = false;

    public static void Init() {
        if (isLoaded) return;

        Path startPath = Minecraft.getInstance().gameDirectory.toPath().resolve("crb");
        try {
            if (!Files.exists(startPath)) {
                Files.createDirectories(startPath);
            }
            createPreview(startPath);
            findAndLoadPacks(startPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        isLoaded = true;
    }

    private static void findAndLoadPacks(Path startPath) {
        try (Stream<Path> stream = Files.list(startPath)) {
            stream.filter(path -> path.toFile().isDirectory()).forEach(path1 -> {
                try {
                    if (path1.getFileName().toString().equals("preview")) return;
                    loadPacks(path1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadPacks(Path path) throws IOException {
        Path json = null;
        Path rpDir = null;
        try (Stream<Path> stream = Files.list(path)) {
            List<Path> files = stream.toList();
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (name.endsWith(".json")) {
                    json = file;
                } else if (Files.isDirectory(file) && name.equals("resourcePack")) {
                    rpDir = file;
                }
            }
        }
        if (json == null || rpDir == null) return;
        ConfigPackData data = GsonHelper.read(json, ConfigPackData.class);
        if (data == null) return;
        ConfigResourcePack resourcePack = new ConfigResourcePack(data.name, rpDir, data.id);
        String disc = String.join("\n", data.description);
        AddResourcePack.addFRP(Minecraft.getInstance().getResourcePackRepository(), resourcePack, Component.literal(disc),
                data.alwaysEnabled, data.position, data.pinned, PackSource.BUILT_IN);
    }

    private static void createPreview(Path startPath) {
        File dirPreview = startPath.resolve("preview").toFile();
        if (!dirPreview.exists()) {
            if (dirPreview.mkdir()) {
                GsonHelper.write(new JsonFile<PreviewConfig>() {
                    @Override
                    public PreviewConfig data() {
                        return new PreviewConfig();
                    }

                    @Override
                    public Path getPath() {
                        return startPath.resolve("preview").resolve("howtocreatecfgpack.json");
                    }
                });
                if (!startPath.resolve("preview").resolve("resourcePack").toFile().exists()) {
                    startPath.resolve("preview").resolve("resourcePack").toFile().mkdir();
                }
            }
        }
    }
}
