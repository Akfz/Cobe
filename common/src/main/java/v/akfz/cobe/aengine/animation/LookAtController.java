package v.akfz.cobe.aengine.animation;

import org.joml.Quaternionf;
import v.akfz.cobe.aengine.data.bone.BoneTransform;

public class LookAtController {
    private String boneName = null;
    private float yaw = 0f;
    private float pitch = 0f;
    private float weight = 0f;
    private boolean isLooking = false;

    public void set(String boneName, float yawDegrees, float pitchDegrees) {
        this.boneName = boneName;
        this.yaw = yawDegrees;
        this.pitch = pitchDegrees;
        this.isLooking = true;
    }

    public void clear() {
        this.isLooking = false;
    }

    public void update(float deltaTimeSec) {
        if (isLooking) {
            this.weight = Math.min(1.0f, this.weight + deltaTimeSec * 5.0f);
        } else {
            this.weight = Math.max(0.0f, this.weight - deltaTimeSec * 5.0f);
        }
    }

    public BoneTransform apply(String currentBoneName, BoneTransform transform) {
        if (this.weight <= 0.0f || this.boneName == null || !this.boneName.equals(currentBoneName)) {
            return transform;
        }

        float yawRad = (float) Math.toRadians(-this.yaw);
        float pitchRad = (float) Math.toRadians(this.pitch);

        Quaternionf lookAtQuat = new Quaternionf()
                .rotationZ(yawRad - 180)
                .rotateX(pitchRad);

        Quaternionf targetRot = new Quaternionf(transform.getRotation()).mul(lookAtQuat);
        Quaternionf blendedRot = new Quaternionf(transform.getRotation()).slerp(targetRot, this.weight);
        return new BoneTransform(transform.getPosition(), blendedRot, transform.getScale());
    }

    public String getBoneName() { return boneName; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getWeight() { return weight; }
    public boolean isLooking() { return isLooking; }
}