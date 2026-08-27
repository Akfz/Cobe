package v.akfz.cobe.core.data.bone;

import v.akfz.cobe.core.data.keyframe.Keyframe;

import java.util.Comparator;
import java.util.List;

//R = renderData
//A = animateData
//R ещё из json читается
public record BoneAData(String boneName, boolean isDeform, List<Keyframe> keyframes) {
    public BoneAData(String boneName, boolean isDeform, List<Keyframe> keyframes) {
        this.boneName = boneName;
        this.isDeform = isDeform;
        if (keyframes != null) {
            keyframes.sort(Comparator.comparingLong(Keyframe::startValue));
        }
        this.keyframes = keyframes;
    }
}
