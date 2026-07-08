package v.akfz.cobe.aengine.animation.calc;

import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import v.akfz.cobe.aengine.animation.AnimatedObject;
import v.akfz.cobe.aengine.animation.util.LookAtController;
import v.akfz.cobe.aengine.data.bone.BoneAData;
import v.akfz.cobe.aengine.data.bone.BoneTransform;
import v.akfz.cobe.aengine.data.cache.AnimatedObjectCache;
import v.akfz.cobe.aengine.animation.keyframe.Keyframe;
import v.akfz.cobe.aengine.data.Transform;
import v.akfz.cobe.aengine.data.cache.AnimationCache;
import v.akfz.cobe.aengine.math.EasingMath;
import v.akfz.cobe.loader.json.animation.Animation;
import v.akfz.cobe.aengine.data.bone.BoneRData;
import v.akfz.cobe.aengine.data.MeshRData;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.loader.json.animation.AnimationsData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AnimationController {
    private final AnimatedObject object;
    public AnimationController(AnimatedObject animatedObject) {
        this.object = animatedObject;
    }

    private final Map<String, AnimationTrack> activeTracks = new ConcurrentHashMap<>();
    private final Queue<QueueEntry> animationInQueue = new ConcurrentLinkedQueue<>();

    private final LookAtController lookAtController = new LookAtController();

    public record QueueEntry(
            String animationName,
            boolean loop,
            boolean hold,
            float transitionTimeSec,
            int layer,
            @Nullable Set<String> boneMask
    ) {}

    public void play(String animationName, boolean loop) {
        play(animationName, loop, false, 0.25f, 0, null);
    }

    public void play(String animationName, boolean loop, boolean hold) {
        play(animationName, loop, hold, 0.25f, 0, null);
    }

    public void play(String animationName, boolean loop, boolean hold, float transitionTimeSec, int layer, @Nullable Set<String> boneMask) {
        Map<String, Animation> cache = AnimationCache.getFromCacheAnimation();
        Animation anim = cache.get(animationName);

        if (anim == null) {
            for (AnimationsData adata : AnimationCache.getFromCacheAnimationData().values()) {
                for (Animation animation : adata.animations) {
                    if (animation.name().equals(animationName)) {
                        anim = animation;
                        cache.put(animationName, animation);
                        break;
                    }
                }
                if (anim != null) {
                    break;
                }
            }
        }

        if (anim == null) {
            return;
        }

        for (AnimationTrack track : activeTracks.values()) {
            if (track.getLayer() == layer && !track.isStopped() && !track.getAnimation().name().equals(animationName)) {
                track.fadeOut(transitionTimeSec);
            }
        }

        if (activeTracks.containsKey(animationName)) {
            AnimationTrack track = activeTracks.get(animationName);
            track.setLoop(loop);
            track.setHoldOnLastFrame(hold);
            track.setLayer(layer);
            track.setBoneMask(boneMask);
            track.fadeIn(transitionTimeSec);
            return;
        }

        AnimationTrack newTrack = new AnimationTrack(anim);
        newTrack.setLoop(loop);
        newTrack.setHoldOnLastFrame(hold);
        newTrack.setLayer(layer);
        newTrack.setBoneMask(boneMask);
        newTrack.fadeIn(transitionTimeSec);

        activeTracks.put(animationName, newTrack);
        AsyncAnimationEngine.getInstance().start();
    }

    public void queue(String animationName, boolean loop) {
        queue(animationName, loop, false, 0.25f, 0, null);
    }

    public void queue(String animationName, boolean loop, boolean hold) {
        queue(animationName, loop, hold, 0.25f, 0, null);
    }

    public void queue(String animationName, boolean loop, boolean hold, float transitionTimeSec, int layer, @Nullable Set<String> boneMask) {
        animationInQueue.add(new QueueEntry(animationName, loop, hold, transitionTimeSec, layer, boneMask));
        AsyncAnimationEngine.getInstance().start();
    }

    public void clearQueue() {
        animationInQueue.clear();
    }

    public void pause(String animationName) {
        if (activeTracks.containsKey(animationName)) activeTracks.get(animationName).setPaused(true);
    }

    public void resume(String animationName) {
        if (activeTracks.containsKey(animationName)) activeTracks.get(animationName).setPaused(false);
    }

    public void stop(String animationName) {
        if (activeTracks.containsKey(animationName)) {
            activeTracks.get(animationName).stop();
            activeTracks.remove(animationName);
        }
    }

    public void stopAll() {
        activeTracks.clear();
        animationInQueue.clear();
    }

    public void update(float deltaTime, List<BoneRData> rootBones, AnimatedObjectCache cache) {
        if (!this.object.shouldPlayAnimationsWhileGamePaused() && Minecraft.getInstance().isPaused()) {
            return;
        }
        boolean hasActive = !activeTracks.isEmpty();

        if (hasActive) {
            activeTracks.entrySet().removeIf(entry -> {
                AnimationTrack track = entry.getValue();
                track.update(deltaTime);
                return track.isStopped();
            });
        }

        processQueue();

        lookAtController.update(deltaTime);

        Matrix4f parentWorldMatrix = new Matrix4f();
        Matrix4f parentRestWorldMatrix = new Matrix4f();

        cache.prepareWrite();

        List<AnimationTrack> sortedTracks = activeTracks.values().stream()
                .sorted(Comparator.comparingInt(AnimationTrack::getLayer))
                .toList();

        for (BoneRData rootBone : rootBones) {
            transformBoneRecursively(rootBone, parentRestWorldMatrix, parentWorldMatrix, sortedTracks, cache);
        }

        cache.publish();
    }

    private void processQueue() {
        if (animationInQueue.isEmpty()) return;

        QueueEntry next = animationInQueue.peek();
        while (next != null) {
            if (!isLayerBusy(next.layer())) {
                animationInQueue.poll();
                play(next.animationName(), next.loop(), next.hold(), next.transitionTimeSec(), next.layer(), next.boneMask());

                next = animationInQueue.peek();
            } else {
                break;
            }
        }
    }

    private boolean isLayerBusy(int layer) {
        for (AnimationTrack track : activeTracks.values()) {
            if (track.getLayer() == layer && !track.isStopped()) {
                return true;
            }
        }
        return false;
    }

    private void transformBoneRecursively(BoneRData bone, Matrix4f parentRestWorldMatrix, Matrix4f parentWorldMatrix, List<AnimationTrack> sortedTracks, AnimatedObjectCache cache) {
        String boneName = bone.name();

        BoneTransform finalTransform = new BoneTransform(bone);

        for (AnimationTrack track : sortedTracks) {
            if (track.getBoneMask() != null && !track.getBoneMask().contains(boneName)) {
                continue;
            }

            Animation animation = track.getAnimation();
            BoneAData boneAnimData = null;
            if (animation != null && animation.bones() != null) {
                for (BoneAData ba : animation.bones()) {
                    if (ba != null && boneName.equals(ba.boneName())) {
                        boneAnimData = ba;
                        break;
                    }
                }
            }

            if (boneAnimData != null) {
                float trackTime = track.getCurrentTime();

                if (animation != null && animation.fps() > 0) {
                    float frameDuration = 1.0f / animation.fps();
                    trackTime = (float) Math.floor(trackTime / frameDuration) * frameDuration;
                }

                BoneTransform trackTransform = calculateInterpolatedTransform(boneAnimData, trackTime);
                finalTransform = BoneTransform.blend(finalTransform, trackTransform, track.getBlendWeight());
            }
        }

        finalTransform = lookAtController.apply(boneName, finalTransform);

        Matrix4f staticLocalMatrix = new Matrix4f()
                .translate(bone.pivot()[0], bone.pivot()[1], bone.pivot()[2]);

        staticLocalMatrix.rotate(new Quaternionf(
                bone.rotation()[0], bone.rotation()[1],
                bone.rotation()[2], bone.rotation()[3]
        ));

        float[] bScale = bone.getScale();
        staticLocalMatrix.scale(bScale[0], bScale[1], bScale[2]);

        Matrix4f boneLocalMatrix = finalTransform.buildMatrix();

        Matrix4f boneWorldMatrix = new Matrix4f(parentWorldMatrix).mul(boneLocalMatrix);
        Matrix4f boneRestWorldMatrix = new Matrix4f(parentRestWorldMatrix).mul(staticLocalMatrix);

        Vector3f headWorld = new Vector3f();
        boneWorldMatrix.getTranslation(headWorld);

        Vector3f tailWorld = new Vector3f();
        boneWorldMatrix.transformPosition(new Vector3f(bone.pivotEnd()[0], bone.pivotEnd()[1], bone.pivotEnd()[2]), tailWorld);

        cache.setMatrix(boneName, boneLocalMatrix);
        cache.setBoneMatrices(boneName, boneLocalMatrix, boneWorldMatrix);
        cache.setBoneRestWorldMatrix(boneName, boneRestWorldMatrix);
        cache.setBonePivots(boneName, headWorld, tailWorld);

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                Matrix4f meshLocalMatrix = new Matrix4f();
                cache.setMeshMatrices(mesh, meshLocalMatrix, boneWorldMatrix);
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                transformBoneRecursively(child, boneRestWorldMatrix, boneWorldMatrix, sortedTracks, cache);
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
            return new BoneTransform(first.data().transform());
        }

        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (currentTime >= last.endValue() / 1000f) {
            return new BoneTransform(last.data().transform());
        }

        Keyframe current = null;
        Keyframe next = null;

        for (int i = 0; i < keyframes.size(); i++) {
            Keyframe kf = keyframes.get(i);
            float start = kf.startValue() / 1000f;
            float end = kf.endValue() / 1000f;

            if (currentTime >= start && currentTime < end) {
                current = kf;
                next = (i + 1 < keyframes.size()) ? keyframes.get(i + 1) : kf;
                break;
            }
        }

        if (current == null) {
            return new BoneTransform(last.data().transform());
        }

        Transform prevTransform = current.data().transform();
        Transform nextTransform = (next != null) ? next.data().transform() : prevTransform;

        float startTime = current.startValue() / 1000f;
        float endTime = current.endValue() / 1000f;

        float alpha = 0f;
        if (endTime > startTime) {
            alpha = (currentTime - startTime) / (endTime - startTime);
            alpha = Math.max(0f, Math.min(1f, alpha));
        }

        switch (current.data().interpolation()) {
            case STEP -> alpha = 0f;
            case BEZIER -> {
                if (current.data().bezierArgs() != null && current.data().bezierArgs().size() == 4) {
                    alpha = getBezierValue(
                            alpha,
                            current.data().bezierArgs().get(0),
                            current.data().bezierArgs().get(1),
                            current.data().bezierArgs().get(2),
                            current.data().bezierArgs().get(3)
                    );
                }
            }
            default -> {
                alpha = EasingMath.apply(alpha, current.data().interpolation(), current.data().easing());
            }
        }

        float posX = lerp(prevTransform.posX(), nextTransform.posX(), alpha);
        float posY = lerp(prevTransform.posY(), nextTransform.posY(), alpha);
        float posZ = lerp(prevTransform.posZ(), nextTransform.posZ(), alpha);

        float scaleX = lerp(prevTransform.scaleX(), nextTransform.scaleX(), alpha);
        float scaleY = lerp(prevTransform.scaleY(), nextTransform.scaleY(), alpha);
        float scaleZ = lerp(prevTransform.scaleZ(), nextTransform.scaleZ(), alpha);

        Quaternionf q0 = new Quaternionf(
                prevTransform.rotX(), prevTransform.rotY(), prevTransform.rotZ(), prevTransform.rotW()
        );

        Quaternionf q1 = new Quaternionf(
                nextTransform.rotX(), nextTransform.rotY(), nextTransform.rotZ(), nextTransform.rotW()
        );

        Quaternionf rotation = new Quaternionf(q0).slerp(q1, alpha);

        return new BoneTransform(new Transform(
                posX, posY, posZ,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scaleX, scaleY, scaleZ
        ));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float getBezierValue(float alpha, float x1, float y1, float x2, float y2) {
        if (alpha <= 0f) return 0f;
        if (alpha >= 1f) return 1f;

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

    public Queue<QueueEntry> getAnimationInQueue() {
        return animationInQueue;
    }

    public LookAtController getLookAt() {
        return lookAtController;
    }
}