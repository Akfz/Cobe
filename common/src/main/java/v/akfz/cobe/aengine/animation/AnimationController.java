package v.akfz.cobe.aengine.animation;

import org.joml.Quaternionf;
import v.akfz.cobe.aengine.data.bone.BoneAData;
import v.akfz.cobe.aengine.data.bone.BoneTransform;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;
import v.akfz.cobe.aengine.animation.keyframe.Keyframe;
import v.akfz.cobe.aengine.data.Transform;
import v.akfz.cobe.aengine.data.cache.AnimationCache;
import v.akfz.cobe.aengine.data.keyframe.KeyframeData;
import v.akfz.cobe.json.animation.Animation;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationController {
    private final Map<String, AnimationTrack> activeTracks = new HashMap<>();

    public void play(String animationName, boolean loop) {
        Animation anim = AnimationCache.CACHED_ANIMATIONS.get(animationName);
        if (anim == null) {
            return;
        }

        if (activeTracks.containsKey(animationName)) {
            AnimationTrack track = activeTracks.get(animationName);
            if (track.isStopped()) {
                track.reset();
            } else if (track.isPaused()) {
                track.setPaused(false);
            }
            return;
        }

        AnimationTrack newTrack = new AnimationTrack(anim);
        newTrack.setLoop(loop);
        activeTracks.put(animationName, newTrack);
        AsyncAnimationEngine.getInstance().start();
    }

    public void pause(String animationName) {
        if (activeTracks.containsKey(animationName)) {
            activeTracks.get(animationName).setPaused(true);
        }
    }

    public void resume(String animationName) {
        if (activeTracks.containsKey(animationName)) {
            activeTracks.get(animationName).setPaused(false);
        }
    }

    public void stop(String animationName) {
        if (activeTracks.containsKey(animationName)) {
            activeTracks.get(animationName).stop();
            activeTracks.remove(animationName);
        }
    }

    public void stopAll() {
        activeTracks.clear();
    }

    public void update(float deltaTime, List<BoneRData> rootBones, AnimatedObjectCache cache) {
        boolean hasActive = !activeTracks.isEmpty();

        if (hasActive) {
            activeTracks.entrySet().removeIf(entry -> {
                AnimationTrack track = entry.getValue();
                track.update(deltaTime);
                return track.isStopped();
            });
            hasActive = !activeTracks.isEmpty();
        }

        Matrix4f parentWorldMatrix = new Matrix4f();
        Matrix4f parentAnimatedMatrix = new Matrix4f();

        Matrix4f parentRestWorldMatrix = new Matrix4f();
        Matrix4f parentRestRotationMatrix = new Matrix4f();

        cache.prepareWrite();

        Animation animation = null;
        float currentTime = 0f;

        if (hasActive) {
            AnimationTrack primaryTrack = activeTracks.values().iterator().next();
            animation = primaryTrack.getAnimation();
            currentTime = primaryTrack.getCurrentTime();
        }

        for (BoneRData rootBone : rootBones) {
            transformBoneRecursively(rootBone, parentRestWorldMatrix, parentRestRotationMatrix, parentWorldMatrix, parentAnimatedMatrix, animation, currentTime, cache);
        }

        cache.publish();
    }

    private void transformBoneRecursively(BoneRData bone, Matrix4f parentRestWorldMatrix, Matrix4f parentRestRotationMatrix, Matrix4f parentWorldMatrix, Matrix4f parentAnimatedMatrix, @Nullable Animation animation, float currentTime, AnimatedObjectCache cache) {
        String boneName = bone.name();

        BoneTransform animTransform = BoneTransform.identity();
        if (animation != null && animation.bones() != null) {
            BoneAData boneData = null;
            for (BoneAData ba : animation.bones()) {
                if (ba != null && boneName.equals(ba.boneName())) {
                    boneData = ba;
                    break;
                }
            }
            if (boneData != null) {
                animTransform = calculateInterpolatedTransform(boneData, currentTime);
            }
        }
        Matrix4f animLocalMatrix = animTransform.buildMatrix();

        // 1. ИСПРАВЛЕНИЕ: Накапливаем абсолютное rest-вращение кости строго по порядку Blender 'XYZ'.
        // При умножении матриц справа для этого нужно последовательно применить Z -> Y -> X.
        Matrix4f restRotationMatrix = new Matrix4f(parentRestRotationMatrix);
        if (bone.rotation() != null) {
            restRotationMatrix
                    .rotateZ((float) Math.toRadians(bone.rotation()[2]))
                    .rotateY((float) Math.toRadians(bone.rotation()[1]))
                    .rotateX((float) Math.toRadians(bone.rotation()[0]));
        }

        // 2. Строим абсолютную rest-матрицу кости в мире
        Matrix4f restWorldMatrix = new Matrix4f()
                .translate(bone.pivot()[0] / 16.0f, bone.pivot()[1] / 16.0f, bone.pivot()[2] / 16.0f)
                .mul(restRotationMatrix);

        // 3. Вычисляем точную относительную rest-матрицу кости к родителю
        Matrix4f staticLocalMatrix = new Matrix4f(parentRestWorldMatrix).invert().mul(restWorldMatrix);

        Matrix4f boneLocalMatrix = new Matrix4f(staticLocalMatrix).mul(animLocalMatrix);

        Matrix4f boneWorldMatrix = new Matrix4f(parentWorldMatrix).mul(boneLocalMatrix);
        Matrix4f boneAnimatedWorldMatrix = new Matrix4f(parentAnimatedMatrix).mul(animLocalMatrix);

        cache.setMatrix(boneName, boneLocalMatrix); // Передаем полную локальную матрицу

        cache.setBoneMatrices(boneName, boneLocalMatrix, boneWorldMatrix);

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                Matrix4f meshLocalMatrix = new Matrix4f();
                Matrix4f meshWorldMatrix = new Matrix4f(boneWorldMatrix);
                cache.setMeshMatrices(mesh, meshLocalMatrix, meshWorldMatrix);
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                transformBoneRecursively(child, restWorldMatrix, restRotationMatrix, boneWorldMatrix, boneAnimatedWorldMatrix, animation, currentTime, cache);
            }
        }
    }

    private BoneTransform calculateInterpolatedTransform(BoneAData boneAData, float currentTime) {
        List<Keyframe> keyframes = boneAData.keyframes();

        if (keyframes == null || keyframes.isEmpty()) {
            return BoneTransform.identity();
        }

        Keyframe first = keyframes.get(0);
        if (currentTime <= first.startValue() / 1000f) {
            return BoneTransform.identity();
        }

        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (currentTime >= last.endValue() / 1000f) {
            return new BoneTransform(last.data().transform());
        }

        Keyframe current = null;
        int currentIndex = -1;

        for (int i = 0; i < keyframes.size(); i++) {
            Keyframe kf = keyframes.get(i);

            float start = kf.startValue() / 1000f;
            float end = kf.endValue() / 1000f;

            if (currentTime >= start && currentTime < end) {
                current = kf;
                currentIndex = i;
                break;
            }
        }

        if (current == null) {
            return new BoneTransform(last.data().transform());
        }

        Transform prev;

        if (currentIndex > 0)
            prev = keyframes.get(currentIndex - 1).data().transform();
        else
            prev = current.data().transform();

        Transform next = current.data().transform();

        float startTime = current.startValue() / 1000f;
        float endTime = current.endValue() / 1000f;

        float alpha = (currentTime - startTime) / (endTime - startTime);
        alpha = Math.max(0f, Math.min(1f, alpha));

        switch (current.data().interpolation()) {
            case STEP -> alpha = 0f;
            case BEZIER -> {
                if (current.data().bezierArgs() != null &&
                        current.data().bezierArgs().size() == 4) {

                    alpha = getBezierValue(
                            alpha,
                            current.data().bezierArgs().get(0),
                            current.data().bezierArgs().get(1),
                            current.data().bezierArgs().get(2),
                            current.data().bezierArgs().get(3)
                    );
                }
            }
        }

        float posX = lerp(prev.posX(), next.posX(), alpha);
        float posY = lerp(prev.posY(), next.posY(), alpha);
        float posZ = lerp(prev.posZ(), next.posZ(), alpha);

        float scaleX = lerp(prev.scaleX(), next.scaleX(), alpha);
        float scaleY = lerp(prev.scaleY(), next.scaleY(), alpha);
        float scaleZ = lerp(prev.scaleZ(), next.scaleZ(), alpha);

        Quaternionf q0 = new Quaternionf(
                prev.rotX(),
                prev.rotY(),
                prev.rotZ(),
                prev.rotW()
        );

        Quaternionf q1 = new Quaternionf(
                next.rotX(),
                next.rotY(),
                next.rotZ(),
                next.rotW()
        );

        Quaternionf rotation = q0.slerp(q1, alpha);

        return new BoneTransform(new Transform(
                posX,
                posY,
                posZ,
                rotation.x,
                rotation.y,
                rotation.z,
                rotation.w,
                scaleX,
                scaleY,
                scaleZ
        ));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float getBezierValue(float alpha, float x1, float y1, float x2, float y2) {
        if (alpha == 0.0 || alpha == 1.0) {
            return alpha;
        }

        float t = alpha;
        for (int i = 0; i < 8; i++) {
            float x = ((1 - t) * (1 - t) * (1 - t) * 0 + 3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t * 1) - alpha;
            float derivative = 3 * (1 - t) * (1 - t) * (x1 - 0) + 6 * (1 - t) * t * (x2 - x1) + 3 * t * t * (1 - x2);
            if (Math.abs(derivative) < 1e-6) break;
            t -= x / derivative;
        }

        return 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t * 1;
    }

    public Map<String, AnimationTrack> getActiveTracks() {
        return activeTracks;
    }
}