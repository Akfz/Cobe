package v.akfz.cobe.aengine.data.bone;

import v.akfz.cobe.aengine.data.Transform;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BoneTransform {

    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);

    public BoneTransform(Transform transform) {
        this.position.set(transform.posX(), transform.posY(), transform.posZ());
        this.rotation.set(transform.rotX(), transform.rotY(), transform.rotZ(), transform.rotW());
        this.scale.set(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }

    public BoneTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position.set(position);
        this.rotation.set(rotation);
        this.scale.set(scale);
    }

    public BoneTransform(BoneRData bone) {
        this.position.set(bone.pivot()[0], bone.pivot()[1], bone.pivot()[2]);
        this.rotation.set(bone.rotation()[0], bone.rotation()[1], bone.rotation()[2], bone.rotation()[3]);
        float[] scl = bone.getScale();
        this.scale.set(scl[0], scl[1], scl[2]);
    }

    public static BoneTransform identity() {
        return new BoneTransform(Transform.identity());
    }

    public static BoneTransform blend(BoneTransform t1, BoneTransform t2, float weight) {
        if (weight <= 0.0f) return t1;
        if (weight >= 1.0f) return t2;

        Vector3f pos = new Vector3f(t1.getPosition()).lerp(t2.getPosition(), weight);
        Quaternionf rot = new Quaternionf(t1.getRotation()).slerp(t2.getRotation(), weight);
        Vector3f scl = new Vector3f(t1.getScale()).lerp(t2.getScale(), weight);

        return new BoneTransform(pos, rot, scl);
    }

    public Matrix4f buildMatrix() {
        return new Matrix4f()
                .translation(position)
                .rotate(rotation)
                .scale(scale);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }
}