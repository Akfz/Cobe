package v.akfz.cobe.loader.json.animation;

import v.akfz.cobe.aengine.data.bone.BoneAData;

import java.util.List;
import java.util.Map;

public record Animation(List<BoneAData> bones, String name, long length, int fps, float speed) {
}
