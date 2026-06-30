package v.akfz.cobe.aengine.data.cache;

import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimatedObjectCache {
    private volatile Map<String, Matrix4f> readBuffer = new ConcurrentHashMap<>();
    private final Map<String, Matrix4f> writeBuffer = new ConcurrentHashMap<>();

    private volatile Map<String, Matrix4f> readBoneLocal = new ConcurrentHashMap<>();
    private volatile Map<String, Matrix4f> readBoneWorld = new ConcurrentHashMap<>();
    private final Map<String, Matrix4f> writeBoneLocal = new ConcurrentHashMap<>();
    private final Map<String, Matrix4f> writeBoneWorld = new ConcurrentHashMap<>();

    private volatile Map<MeshRData, Matrix4f> readMeshLocal = new ConcurrentHashMap<>();
    private volatile Map<MeshRData, Matrix4f> readMeshWorld = new ConcurrentHashMap<>();
    private final Map<MeshRData, Matrix4f> writeMeshLocal = new ConcurrentHashMap<>();
    private final Map<MeshRData, Matrix4f> writeMeshWorld = new ConcurrentHashMap<>();

    private volatile List<BoneRData> rootBones;

    public void prepareWrite() {
        writeBuffer.clear();
        writeBoneLocal.clear();
        writeBoneWorld.clear();
        writeMeshLocal.clear();
        writeMeshWorld.clear();
    }

    public void setMatrix(String boneName, Matrix4f matrix) {
        writeBuffer.put(boneName, matrix);
    }

    @Nullable
    public Matrix4f getMatrix(String boneName) {
        return readBuffer.get(boneName);
    }

    public void setBoneMatrices(String boneName, Matrix4f local, Matrix4f world) {
        writeBoneLocal.put(boneName, local);
        writeBoneWorld.put(boneName, world);
    }

    public void setMeshMatrices(MeshRData mesh, Matrix4f local, Matrix4f world) {
        writeMeshLocal.put(mesh, local);
        writeMeshWorld.put(mesh, world);
    }

    public Matrix4f getBoneLocalMatrix(String boneName) {
        return readBoneLocal.getOrDefault(boneName, new Matrix4f());
    }

    public Matrix4f getBoneWorldMatrix(String boneName) {
        return readBoneWorld.getOrDefault(boneName, new Matrix4f());
    }

    public Matrix4f getMeshLocalMatrix(MeshRData mesh) {
        return readMeshLocal.getOrDefault(mesh, new Matrix4f());
    }

    public Matrix4f getMeshWorldMatrix(MeshRData mesh) {
        return readMeshWorld.getOrDefault(mesh, new Matrix4f());
    }

    public void publish() {
        this.readBuffer = deepCopyMapString(writeBuffer);
        this.readBoneLocal = deepCopyMapString(writeBoneLocal);
        this.readBoneWorld = deepCopyMapString(writeBoneWorld);
        this.readMeshLocal = deepCopyMapMesh(writeMeshLocal);
        this.readMeshWorld = deepCopyMapMesh(writeMeshWorld);
    }

    private Map<String, Matrix4f> deepCopyMapString(Map<String, Matrix4f> original) {
        Map<String, Matrix4f> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, Matrix4f> entry : original.entrySet()) {
            copy.put(entry.getKey(), new Matrix4f(entry.getValue()));
        }
        return copy;
    }

    private Map<MeshRData, Matrix4f> deepCopyMapMesh(Map<MeshRData, Matrix4f> original) {
        Map<MeshRData, Matrix4f> copy = new ConcurrentHashMap<>();
        for (Map.Entry<MeshRData, Matrix4f> entry : original.entrySet()) {
            copy.put(entry.getKey(), new Matrix4f(entry.getValue()));
        }
        return copy;
    }

    public void clear() {
        readBuffer.clear();
        readBoneLocal.clear();
        readBoneWorld.clear();
        readMeshLocal.clear();
        readMeshWorld.clear();
        prepareWrite();
    }

    public Map<String, Matrix4f> getAllMatrices() {
        return readBuffer;
    }

    @Nullable
    public List<BoneRData> getRootBones() {
        return this.rootBones;
    }

    public void setRootBones(List<BoneRData> rootBones) {
        this.rootBones = rootBones;
    }
}