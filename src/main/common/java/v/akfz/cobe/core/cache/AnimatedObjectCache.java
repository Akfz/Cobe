package v.akfz.cobe.core.cache;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.bone.BoneRData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedObjectCache {

    public static final class PoseBuffer {
        public final Map<String, Matrix4f> boneLocal = new HashMap<>();
        public final Map<String, Matrix4f> boneWorld = new HashMap<>();
        public final Map<String, Matrix4f> boneRestWorld = new HashMap<>();
        public final Map<String, Matrix4f> boneSkin = new HashMap<>();
        public final Map<String, Vector3f> boneHead = new HashMap<>();
        public final Map<String, Vector3f> boneTail = new HashMap<>();
        public final Map<MeshRData, Matrix4f> meshLocal = new HashMap<>();
        public final Map<MeshRData, Matrix4f> meshWorld = new HashMap<>();

        public Matrix4f[] indexedSkinMatrices = new Matrix4f[64];

        public final Map<MeshRData, float[]> skinnedMeshBuffers = new HashMap<>();

        public void ensureMatrixCapacity(int count) {
            if (indexedSkinMatrices.length < count) {
                Matrix4f[] next = new Matrix4f[Math.max(indexedSkinMatrices.length * 2, count)];
                System.arraycopy(indexedSkinMatrices, 0, next, 0, indexedSkinMatrices.length);
                for (int i = indexedSkinMatrices.length; i < next.length; i++) {
                    next[i] = new Matrix4f();
                }
                indexedSkinMatrices = next;
            }
        }
    }

    private final PoseBuffer bufferA = new PoseBuffer();
    private final PoseBuffer bufferB = new PoseBuffer();

    private volatile PoseBuffer readBuffer = bufferA;
    private PoseBuffer writeBuffer = bufferB;

    private volatile List<BoneRData> rootBones;
    private final Map<String, Integer> boneNameToId = new HashMap<>();
    private int nextBoneId = 0;

    public synchronized int getOrRegisterBoneId(String boneName) {
        return boneNameToId.computeIfAbsent(boneName, k -> nextBoneId++);
    }

    public void setBoneMatrices(String boneName, Matrix4f local, Matrix4f world) {
        if (boneName == null) return;
        writeBuffer.boneLocal.computeIfAbsent(boneName, k -> new Matrix4f()).set(local);
        writeBuffer.boneWorld.computeIfAbsent(boneName, k -> new Matrix4f()).set(world);
    }

    public void setBoneRestWorldMatrix(String boneName, Matrix4f restWorld) {
        if (boneName == null) return;
        writeBuffer.boneRestWorld.computeIfAbsent(boneName, k -> new Matrix4f()).set(restWorld);
    }

    public void setBoneSkinMatrix(String boneName, Matrix4f skin) {
        if (boneName == null) return;
        writeBuffer.boneSkin.computeIfAbsent(boneName, k -> new Matrix4f()).set(skin);

        int id = getOrRegisterBoneId(boneName);
        writeBuffer.ensureMatrixCapacity(id + 1);
        if (writeBuffer.indexedSkinMatrices[id] == null) {
            writeBuffer.indexedSkinMatrices[id] = new Matrix4f();
        }
        writeBuffer.indexedSkinMatrices[id].set(skin);
    }

    public void setBonePivots(String boneName, Vector3f head, Vector3f tail) {
        if (boneName == null) return;
        writeBuffer.boneHead.computeIfAbsent(boneName, k -> new Vector3f()).set(head);
        writeBuffer.boneTail.computeIfAbsent(boneName, k -> new Vector3f()).set(tail);
    }

    public void setMeshMatrices(MeshRData mesh, Matrix4f local, Matrix4f world) {
        if (mesh == null) return;
        writeBuffer.meshLocal.computeIfAbsent(mesh, k -> new Matrix4f()).set(local);
        writeBuffer.meshWorld.computeIfAbsent(mesh, k -> new Matrix4f()).set(world);
    }

    public void publish() {
        PoseBuffer temp = readBuffer;
        readBuffer = writeBuffer;
        writeBuffer = temp;
    }

    public Matrix4f getBoneLocalMatrix(String boneName) {
        if (boneName == null) return new Matrix4f();
        Matrix4f m = readBuffer.boneLocal.get(boneName);
        return m != null ? m : new Matrix4f();
    }

    public Map<String, Matrix4f> getAllBoneLocalMatrices() {
        return readBuffer.boneLocal;
    }

    public Matrix4f getBoneWorldMatrix(String boneName) {
        if (boneName == null) return new Matrix4f();
        Matrix4f m = readBuffer.boneWorld.get(boneName);
        return m != null ? m : new Matrix4f();
    }

    public Map<String, Matrix4f> getAllBoneWorldMatrices() {
        return readBuffer.boneWorld;
    }

    public Matrix4f getBoneRestWorldMatrix(String boneName) {
        if (boneName == null) return new Matrix4f();
        Matrix4f m = readBuffer.boneRestWorld.get(boneName);
        return m != null ? m : new Matrix4f();
    }

    public Map<String, Matrix4f> getAllBoneRestWorldMatrices() {
        return readBuffer.boneRestWorld;
    }

    @Nullable
    public Matrix4f getBoneSkinMatrix(String boneName) {
        if (boneName == null) return null;
        return readBuffer.boneSkin.get(boneName);
    }

    public Map<String, Matrix4f> getAllBoneSkinMatrices() {
        return readBuffer.boneSkin;
    }

    public Matrix4f getMeshLocalMatrix(MeshRData mesh) {
        if (mesh == null) return new Matrix4f();
        Matrix4f m = readBuffer.meshLocal.get(mesh);
        return m != null ? m : new Matrix4f();
    }

    public Map<MeshRData, Matrix4f> getAllMeshLocalMatrices() {
        return readBuffer.meshLocal;
    }

    public Matrix4f getMeshWorldMatrix(MeshRData mesh) {
        if (mesh == null) return new Matrix4f();
        Matrix4f m = readBuffer.meshWorld.get(mesh);
        return m != null ? m : new Matrix4f();
    }

    public Map<MeshRData, Matrix4f> getAllMeshWorldMatrices() {
        return readBuffer.meshWorld;
    }

    public Vector3f getBoneHead(String boneName) {
        if (boneName == null) return new Vector3f();
        Vector3f v = readBuffer.boneHead.get(boneName);
        return v != null ? v : new Vector3f();
    }

    public Map<String, Vector3f> getAllBoneHead() {
        return readBuffer.boneHead;
    }

    public Vector3f getBoneTail(String boneName) {
        if (boneName == null) return new Vector3f();
        Vector3f v = readBuffer.boneTail.get(boneName);
        return v != null ? v : new Vector3f();
    }

    public Map<String, Vector3f> getAllBoneTail() {
        return readBuffer.boneTail;
    }

    public Matrix4f[] getIndexedSkinMatrices() {
        return readBuffer.indexedSkinMatrices;
    }

    public Matrix4f[] getWriteIndexedSkinMatrices() {
        return writeBuffer.indexedSkinMatrices;
    }

    @Nullable
    public float[] getSkinnedVertices(MeshRData mesh) {
        return readBuffer.skinnedMeshBuffers.get(mesh);
    }

    public float[] getOrCreateWriteSkinnedBuffer(MeshRData mesh, int size) {
        float[] buf = writeBuffer.skinnedMeshBuffers.get(mesh);
        if (buf == null || buf.length < size) {
            buf = new float[size];
            writeBuffer.skinnedMeshBuffers.put(mesh, buf);
        }
        return buf;
    }

    public List<BoneRData> getRootBones() {
        return this.rootBones;
    }

    public void setRootBones(List<BoneRData> rootBones) {
        this.rootBones = rootBones;
    }
}