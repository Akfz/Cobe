package v.akfz.cobe.aengine.data;

import v.akfz.cobe.aengine.animation.AnimatedObject;
import org.joml.Matrix4f;
import java.util.List;

public record MeshRData(
        List<float[]> vertices,
        List<float[]> uvs,
        List<FaceData> faces
) {
    public record FaceData(
            int[] vertexIndices,
            int[] uvIndices
    ) {}

    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshLocalMatrix(this);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshWorldMatrix(this);
    }
}