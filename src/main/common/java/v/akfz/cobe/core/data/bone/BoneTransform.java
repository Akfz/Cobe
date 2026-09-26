package v.akfz.cobe.core.data.bone;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import v.akfz.cobe.core.data.Transform;

public class BoneTransform {

    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);

    public BoneTransform() {
    }

    public BoneTransform(Transform transform) {
        if (transform == null) {
            return;
        }

        this.position.set(transform.posX(), transform.posY(), transform.posZ());
        this.rotation.set(transform.rotX(), transform.rotY(), transform.rotZ(), transform.rotW());
        this.scale.set(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }

    public BoneTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        if (position != null) {
            this.position.set(position);
        }

        if (rotation != null) {
            this.rotation.set(rotation);
        }

        if (scale != null) {
            this.scale.set(scale);
        }
    }

    public BoneTransform(BoneRData bone) {
        if (bone == null) {
            return;
        }

        this.position.set(bone.pivot()[0], bone.pivot()[1], bone.pivot()[2]);
        this.rotation.set(
                bone.rotation()[0],
                bone.rotation()[1],
                bone.rotation()[2],
                bone.rotation()[3]
        );

        float[] scale = bone.getScale();
        this.scale.set(scale[0], scale[1], scale[2]);
    }

    public BoneTransform(BoneTransform other) {
        if (other == null) {
            return;
        }

        this.position.set(other.position);
        this.rotation.set(other.rotation);
        this.scale.set(other.scale);
    }

    public static BoneTransform identity() {
        return new BoneTransform();
    }

    public static BoneTransform blend(BoneTransform a, BoneTransform b, float weight) {
        if (a == null) {
            return b == null ? identity() : b;
        }

        if (b == null) {
            return a;
        }

        if (weight <= 0.0f) {
            return a;
        }

        if (weight >= 1.0f) {
            return b;
        }

        BoneTransform out = new BoneTransform();

        out.position.set(a.position).lerp(b.position, weight);
        out.rotation.set(a.rotation).slerp(b.rotation, weight);
        out.scale.set(a.scale).lerp(b.scale, weight);

        return out;
    }

    public Matrix4f buildMatrix() {
        return buildMatrix(new Matrix4f());
    }

    public Matrix4f buildMatrix(Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }

        dest.translation(position);
        dest.rotate(rotation);
        dest.scale(scale);

        return dest;
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

    public Vector3f positionDirect() {
        return position;
    }

    public Quaternionf rotationDirect() {
        return rotation;
    }

    public Vector3f scaleDirect() {
        return scale;
    }
}