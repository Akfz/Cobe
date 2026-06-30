package v.akfz.cobe.json.model;

import n.paradox.aslib.util.json.JsonData;
import v.akfz.cobe.aengine.data.bone.BoneRData;

import java.util.List;

public class ModelData implements JsonData {
    //public int loaderId = 0; // я не помню че это
    public List<BoneTexture> texturePaths; // можно и желательно вручную выставлять(в коде, но в блендере есть спец. фича)
    public List<BoneRData> bones;

    @Override
    public String toString() {
        return "texturePaths : " + texturePaths.toString() + "bones : " + bones;
    }
}
