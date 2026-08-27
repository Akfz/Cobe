package v.akfz.cobe.core.data;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public record Transform(
        float posX, float posY, float posZ,
        float rotX, float rotY, float rotZ, float rotW,
        float scaleX, float scaleY, float scaleZ
) {

    public static Transform identity() {
        return new Transform(
                0f, 0f, 0f,
                0f, 0f, 0f, 1f,
                1f, 1f, 1f
        );
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotX, rotY, rotZ, rotW);
    }

    public Vector3f getPosition() {
        return new Vector3f(posX, posY, posZ);
    }

    public Vector3f getScale() {
        return new Vector3f(scaleX, scaleY, scaleZ);
    }

    public static Transform of(Quaternionf rotation, Vector3f position, Vector3f scale) {
        return new Transform(
                position.x, position.y, position.z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scale.x, scale.y, scale.z
        );
    }
}
