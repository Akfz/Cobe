package v.akfz.cobe.loader.resourcepack.configpack.preview;

import v.akfz.cobe.loader.resourcepack.configpack.ConfigData;

import java.util.List;

public class PreviewConfig implements ConfigData {
    public String name = "change to name|замени для имени";
    public String id = "like mod id, for use in game id:path | как мод айди, для использования в игре id:путь";
    public String alwaysEnabled = "if true cant be turned off | если true, невозможно выключить";
    public List<String> description = List.of("ENG : this file create in minecraft resourcepack with files inside this folder...","WARNING IF U ARE LOADING FROM CONFIG PACK bone textures need to be setted like : id:path", "RU : этот файл создает в майнкрафте ресурс пак с файлами внутри папки..","ВНИМАНИЕ, ЕСЛИ ЗАГРУЖАТЬ ЧЕРЕЗ КОНФИГ ПАК, ТО ТЕКСТУРЫ КОСТЕЙ НУЖНО ЗАГРУЖАТЬ КАК id:путь", "\uD83D\uDE0F");
    public String pinned = "can change pos in pack list (in minecraft settings) | можно ли менять позицию в пак листа (в майкрафт настройках)";
    public String position = "only TOP or BOTTOM, set in pack list | только TOP или BOTTOM, ставит в пак листе либо в самым первым(TOP) или в конце(BOTTOM)";
}
