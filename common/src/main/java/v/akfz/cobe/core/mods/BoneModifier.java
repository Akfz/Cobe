package v.akfz.cobe.core.mods;

import v.akfz.cobe.core.data.bone.BoneTransform;

@FunctionalInterface
public interface BoneModifier {
    BoneTransform apply(String boneName, BoneTransform currentTransform);
}
