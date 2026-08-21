package v.akfz.cobe.aengine.data;

import v.akfz.cobe.aengine.animation.AnimatedObject;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public record MeshRData(
        List<float[]> vertices,
        List<float[]> uvs,
        List<FaceData> faces,
        @Nullable List<SkinningData> skinningData
) {
    public record FaceData(
            int[] vertexIndices,
            int[] uvIndices
    ) {}

    public record SkinningData(
            String[] joints,
            float[] weights
    ) {}

    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshLocalMatrix(this);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshWorldMatrix(this);
    }

    public boolean isSkinned() {
        return skinningData != null && !skinningData.isEmpty();
    }
}