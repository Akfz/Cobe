package v.akfz.cobe.core.data;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import v.akfz.cobe.core.object.AnimatedObject;

import java.util.List;

public record MeshRData(
        List<float[]> vertices,
        List<float[]> uvs,
        List<FaceData> faces,
        @Nullable List<SkinningData> skinningData,
        float[] precomputedFaceNormals
) {
    public MeshRData(List<float[]> vertices, List<float[]> uvs, List<FaceData> faces, @Nullable List<SkinningData> skinningData) {
        this(vertices, uvs, faces, skinningData, precomputeNormals(vertices, faces));
    }

    public record FaceData(
            int[] vertexIndices,
            int[] uvIndices
    ) {}

    public record SkinningData(
            String[] joints,
            float[] weights,
            int[] jointIndices
    ) {
        public SkinningData(String[] joints, float[] weights) {
            this(joints, weights, new int[joints != null ? joints.length : 0]);
        }
    }

    private static float[] precomputeNormals(List<float[]> vertices, List<FaceData> faces) {
        if (faces == null || vertices == null) return new float[0];
        float[] normals = new float[faces.size() * 3];

        for (int i = 0; i < faces.size(); i++) {
            FaceData face = faces.get(i);
            int[] vIdx = face.vertexIndices();
            if (vIdx.length < 3) continue;

            float[] v0 = vertices.get(vIdx[0]);
            float[] v1 = vertices.get(vIdx[1]);
            float[] v2 = vertices.get(vIdx[2]);

            float e1x = v1[0] - v0[0], e1y = v1[1] - v0[0], e1z = v1[2] - v0[2];
            float e2x = v2[0] - v0[0], e2y = v2[1] - v0[1], e2z = v2[2] - v0[2];

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

            int nIdx = i * 3;
            normals[nIdx]     = nx;
            normals[nIdx + 1] = ny;
            normals[nIdx + 2] = nz;
        }
        return normals;
    }

    public boolean isSkinned() {
        return skinningData != null && !skinningData.isEmpty();
    }
}