package v.akfz.cobe.aengine.data.bone;

import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.data.MeshRData;
import org.joml.Matrix4f;
import java.util.List;

//R = renderData
//A = animateData
//R ещё из json читается
public record BoneRData(
        String name,
        float[] pivot,
        float[] rotation,
        List<MeshRData> meshes,
        List<BoneRData> children
) {
    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getBoneLocalMatrix(this.name);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getBoneWorldMatrix(this.name);
    }
}