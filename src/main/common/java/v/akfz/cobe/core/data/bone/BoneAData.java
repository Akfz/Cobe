package v.akfz.cobe.core.data.bone;

import v.akfz.cobe.core.data.keyframe.Keyframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//R = renderData
//A = animateData
//R reads from JSON
public record BoneAData(String boneName, boolean isDeform, List<Keyframe> keyframes) {
    public BoneAData(String boneName, boolean isDeform, List<Keyframe> keyframes) {
        this.boneName = boneName;
        this.isDeform = isDeform;
        if (keyframes == null) {
            this.keyframes = null;
        } else {
            List<Keyframe> sorted = new ArrayList<>(keyframes);
            sorted.sort(Comparator.comparingLong(Keyframe::startValue));
            this.keyframes = Collections.unmodifiableList(sorted);
        }
    }
}