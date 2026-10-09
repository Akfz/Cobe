package v.akfz.cobe.core.data;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import v.akfz.cobe.core.object.AnimatedObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record MeshRData(
        List<float[]> vertices,
        List<float[]> uvs,
        List<FaceData> faces,
        @Nullable List<SkinningData> skinningData
) {
    private static final Map<MeshRData, float[]> BAKED_CACHE = new ConcurrentHashMap<>();

    public record FaceData(
            int[] vertexIndices,
            int[] uvIndices
    ) {}

    public record SkinningData(
            String[] joints,
            float[] weights
    ) {}

    public boolean isSkinned() {
        return skinningData != null && !skinningData.isEmpty();
    }

    public Matrix4f getLocalMatrix(AnimatedObject animated) {
        if (animated == null || animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshLocalMatrix(this);
    }

    public Matrix4f getWorldMatrix(AnimatedObject animated) {
        if (animated == null || animated.getCache() == null) return new Matrix4f();
        return animated.getCache().getMeshWorldMatrix(this);
    }

    public float[] getBakedQuadData() {
        return BAKED_CACHE.computeIfAbsent(this, MeshRData::bakeQuads);
    }

    private static float[] bakeQuads(MeshRData mesh) {
        List<float[]> vertices = mesh.vertices();
        List<float[]> uvs = mesh.uvs();
        List<FaceData> faces = mesh.faces();
        if (faces == null || vertices == null || uvs == null) return new float[0];

        int totalFloats = countBakedVertices(faces) * 8;
        float[] buffer = new float[totalFloats];
        int offset = 0;

        for (FaceData face : faces) {
            int[] vIdx = face.vertexIndices();
            int[] uvIdx = face.uvIndices();
            int count = vIdx != null ? vIdx.length : 0;
            if (count < 3) continue;

            float[] v0 = vertices.get(vIdx[0]);
            float[] v1 = vertices.get(vIdx[1]);
            float[] v2 = vertices.get(vIdx[2]);

            float e1x = v1[0] - v0[0], e1y = v1[1] - v0[0], e1z = v1[2] - v0[2];
            float e2x = v2[0] - v0[0], e2y = v2[1] - v0[0], e2z = v2[2] - v0[2];

            float nx = e2y * e1z - e2z * e1y;
            float ny = e2z * e1x - e2x * e1z;
            float nz = e2x * e1y - e2y * e1x;

            float lenSq = nx * nx + ny * ny + nz * nz;
            if (lenSq > 1e-6f) {
                float invLen = (float) (1.0 / Math.sqrt(lenSq));
                nx *= invLen; ny *= invLen; nz *= invLen;
            } else {
                nx = 0f; ny = 1f; nz = 0f;
            }

            if (count == 4) {
                offset = putVertex(buffer, offset, vertices.get(vIdx[0]), getUvSafe(uvs, uvIdx, 0), nx, ny, nz);
                offset = putVertex(buffer, offset, vertices.get(vIdx[3]), getUvSafe(uvs, uvIdx, 3), nx, ny, nz);
                offset = putVertex(buffer, offset, vertices.get(vIdx[2]), getUvSafe(uvs, uvIdx, 2), nx, ny, nz);
                offset = putVertex(buffer, offset, vertices.get(vIdx[1]), getUvSafe(uvs, uvIdx, 1), nx, ny, nz);
            } else {
                for (int i = 1; i < count - 1; i++) {
                    offset = putVertex(buffer, offset, vertices.get(vIdx[0]),     getUvSafe(uvs, uvIdx, 0),     nx, ny, nz);
                    offset = putVertex(buffer, offset, vertices.get(vIdx[i + 1]), getUvSafe(uvs, uvIdx, i + 1), nx, ny, nz);
                    offset = putVertex(buffer, offset, vertices.get(vIdx[i]),     getUvSafe(uvs, uvIdx, i),     nx, ny, nz);
                    offset = putVertex(buffer, offset, vertices.get(vIdx[i]),     getUvSafe(uvs, uvIdx, i),     nx, ny, nz);
                }
            }
        }
        return buffer;
    }

    private static float[] getUvSafe(List<float[]> uvs, int[] uvIdx, int step) {
        if (uvIdx != null && step < uvIdx.length && uvIdx[step] < uvs.size()) {
            return uvs.get(uvIdx[step]);
        }
        return new float[]{0.0f, 0.0f};
    }

    private static int putVertex(float[] buffer, int offset, float[] pos, float[] uv, float nx, float ny, float nz) {
        buffer[offset]     = pos[0];
        buffer[offset + 1] = pos[1];
        buffer[offset + 2] = pos[2];
        buffer[offset + 3] = uv[0];
        buffer[offset + 4] = uv[1];
        buffer[offset + 5] = nx;
        buffer[offset + 6] = ny;
        buffer[offset + 7] = nz;
        return offset + 8;
    }

    private static int countBakedVertices(List<FaceData> faces) {
        if (faces == null) return 0;
        int count = 0;
        for (FaceData f : faces) {
            int vCount = f.vertexIndices() != null ? f.vertexIndices().length : 0;
            if (vCount == 4) count += 4;
            else if (vCount >= 3) count += (vCount - 2) * 4;
        }
        return count;
    }
}