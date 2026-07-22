package v.akfz.cobe.aengine.animation.calc;

import v.akfz.cobe.aengine.data.bone.BoneTransform;

@FunctionalInterface
public interface BoneModifier {
    BoneTransform apply(String boneName, BoneTransform currentTransform);
}