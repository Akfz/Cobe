package v.akfz.cobe.aengine.data.bone;

import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.data.MeshRData;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public record BoneRData(
        String name,
        float[] pivot,
        float[] pivotEnd,
        float[] rotation,
        float @Nullable [] scale,
        BoneRenderTypes renderTypes,
        List<MeshRData> meshes,
        List<BoneRData> children
) {
    public float[] getScale() {
        return scale != null ? scale : new float[]{1.0f, 1.0f, 1.0f};
    }

    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getBoneLocalMatrix(this.name);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getBoneWorldMatrix(this.name);
    }
}