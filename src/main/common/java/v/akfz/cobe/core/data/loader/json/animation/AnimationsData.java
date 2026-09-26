package v.akfz.cobe.core.data.loader.json.animation;

import v.akfz.aslib.util.json.JsonData;
import v.akfz.cobe.core.data.Animation;

import java.util.List;

public class AnimationsData implements JsonData {
    public int loadVer;
    public List<Animation> animations;
}
