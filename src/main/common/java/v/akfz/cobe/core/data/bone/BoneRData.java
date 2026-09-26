package v.akfz.cobe.core.data.bone;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.object.AnimatedObject;

import java.util.List;

public record BoneRData(
        String name,
        float[] pivot,
        float[] pivotEnd,
        float[] rotation,
        float @Nullable [] scale,
        List<MeshRData> meshes,
        List<BoneRData> children
) {
    private static final float[] DEFAULT_SCALE = new float[]{1.0f, 1.0f, 1.0f};

    public float[] getScale() {
        return scale != null ? scale : DEFAULT_SCALE;
    }

    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated == null || animated.getCache() == null) {
            return new Matrix4f();
        }
        return animated.getCache().getBoneLocalMatrix(this.name);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated == null || animated.getCache() == null) {
            return new Matrix4f();
        }
        return animated.getCache().getBoneWorldMatrix(this.name);
    }
}