package v.akfz.cobe.core.data;

import com.google.gson.annotations.SerializedName;
import v.akfz.cobe.core.data.bone.BoneAData;
import v.akfz.cobe.core.data.keyframe.FpsKeyframe;

import java.util.List;

public record Animation(
        String name,
        long length,
        @SerializedName("fps") int startFps,
        float speed,
        List<BoneAData> bones,
        @SerializedName("fpsKeyframes") List<FpsKeyframe> fpsKeyframes
) {
}