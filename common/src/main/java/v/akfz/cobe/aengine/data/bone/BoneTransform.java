package v.akfz.cobe.aengine.data.bone;

import v.akfz.cobe.aengine.data.Transform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BoneTransform {

    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);

    public BoneTransform(Transform transform) {
        this.position.set(
                transform.posX() / 16.0f,
                transform.posY() / 16.0f,
                transform.posZ() / 16.0f
        );

        this.rotation.set(
                transform.rotX(),
                transform.rotY(),
                transform.rotZ(),
                transform.rotW()
        );

        this.scale.set(
                transform.scaleX(),
                transform.scaleY(),
                transform.scaleZ()
        );
    }

    public static BoneTransform identity() {
        return new BoneTransform(Transform.identity());
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