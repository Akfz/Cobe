package v.akfz.cobe.core.cache;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.bone.BoneRData;
import java.util.List;
import java.util.Map;

public class AnimatedObjectCache {
    public record PoseSnapshot(
            Map<String, Matrix4f> boneLocal,
            Map<String, Matrix4f> boneWorld,
            Map<String, Matrix4f> boneRestWorld,
            Map<String, Matrix4f> boneSkin,
            Map<String, Vector3f> boneHead,
            Map<String, Vector3f> boneTail,
            Map<MeshRData, Matrix4f> meshLocal,
            Map<MeshRData, Matrix4f> meshWorld
    ) {}

    private volatile PoseSnapshot previousSnapshot = new PoseSnapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    private volatile PoseSnapshot currentSnapshot = new PoseSnapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    private final Map<String, Matrix4f> writeBoneLocal = new java.util.HashMap<>();
    private final Map<String, Matrix4f> writeBoneWorld = new java.util.HashMap<>();
    private final Map<String, Matrix4f> writeBoneRestWorld = new java.util.HashMap<>();
    private final Map<String, Matrix4f> writeBoneSkin = new java.util.HashMap<>();
    private final Map<String, Vector3f> writeBoneHead = new java.util.HashMap<>();
    private final Map<String, Vector3f> writeBoneTail = new java.util.HashMap<>();
    private final Map<MeshRData, Matrix4f> writeMeshLocal = new java.util.HashMap<>();
    private final Map<MeshRData, Matrix4f> writeMeshWorld = new java.util.HashMap<>();

    private volatile List<BoneRData> rootBones;

    public void prepareWrite() {
        writeBoneLocal.clear(); writeBoneWorld.clear(); writeBoneRestWorld.clear(); writeBoneSkin.clear();
        writeBoneHead.clear(); writeBoneTail.clear(); writeMeshLocal.clear(); writeMeshWorld.clear();
    }

    public void setBoneMatrices(String boneName, Matrix4f local, Matrix4f world) {
        if (boneName == null) return;
        writeBoneLocal.put(boneName, local);
        writeBoneWorld.put(boneName, world);
    }

    public void setBoneRestWorldMatrix(String boneName, Matrix4f restWorld) {
        if (boneName != null) writeBoneRestWorld.put(boneName, restWorld);
    }

    public void setBoneSkinMatrix(String boneName, Matrix4f skin) {
        if (boneName != null) writeBoneSkin.put(boneName, skin);
    }

    public void setBonePivots(String boneName, Vector3f head, Vector3f tail) {
        if (boneName == null) return;
        writeBoneHead.put(boneName, head); writeBoneTail.put(boneName, tail);
    }

    public void setMeshMatrices(MeshRData mesh, Matrix4f local, Matrix4f world) {
        if (mesh == null) return;
        writeMeshLocal.put(mesh, local); writeMeshWorld.put(mesh, world);
    }

    public void publish() {
        this.previousSnapshot = this.currentSnapshot;
        this.currentSnapshot = new PoseSnapshot(
                Map.copyOf(writeBoneLocal), Map.copyOf(writeBoneWorld), Map.copyOf(writeBoneRestWorld),
                Map.copyOf(writeBoneSkin),
                Map.copyOf(writeBoneHead), Map.copyOf(writeBoneTail),
                Map.copyOf(writeMeshLocal), Map.copyOf(writeMeshWorld)
        );
    }

    public PoseSnapshot getSnapshot() { return currentSnapshot; }

    public Matrix4f getBoneLocalMatrix(String boneName) {
        if (boneName == null) return new Matrix4f();
        return currentSnapshot.boneLocal().getOrDefault(boneName, new Matrix4f());
    }

    public Matrix4f getBoneWorldMatrix(String boneName) {
        return currentSnapshot.boneWorld().getOrDefault(boneName, new Matrix4f());
    }

    public Matrix4f getBoneRestWorldMatrix(String boneName) {
        return currentSnapshot.boneRestWorld().getOrDefault(boneName, new Matrix4f());
    }

    public Matrix4f getBoneSkinMatrix(String boneName) {
        return currentSnapshot.boneSkin().get(boneName);
    }

    public Matrix4f getMeshLocalMatrix(MeshRData mesh) {
        return currentSnapshot.meshLocal().getOrDefault(mesh, new Matrix4f());
    }
    public Matrix4f getMeshWorldMatrix(MeshRData mesh) {
        return currentSnapshot.meshWorld().getOrDefault(mesh, new Matrix4f());
    }
    public List<BoneRData> getRootBones() {
        return this.rootBones;
    }
    public void setRootBones(List<BoneRData> rootBones) {
        this.rootBones = rootBones;
    }
}