package v.akfz.cobe.core.animation;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.cache.AnimationCache;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.Transform;
import v.akfz.cobe.core.data.bone.BoneAData;
import v.akfz.cobe.core.data.bone.BoneRData;
import v.akfz.cobe.core.data.bone.BoneTransform;
import v.akfz.cobe.core.data.keyframe.Easing;
import v.akfz.cobe.core.data.keyframe.InterpolationType;
import v.akfz.cobe.core.data.keyframe.Keyframe;
import v.akfz.cobe.core.event.AnimationEvent;
import v.akfz.cobe.core.event.AnimationEventSink;
import v.akfz.cobe.core.math.EasingMath;
import v.akfz.cobe.core.mods.BoneModifier;
import v.akfz.cobe.core.object.AnimatedObject;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnimationController {

    private final AnimatedObject object;

    private final Map<String, AnimationTrack> activeTracks = new HashMap<>();
    private final Deque<QueueEntry> animationQueue = new ArrayDeque<>();
    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final Queue<AnimationEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final List<BoneModifier> boneModifiers = new CopyOnWriteArrayList<>();

    private final List<AnimationTrack> sortedTracksCache = new ArrayList<>();
    private boolean tracksDirty = true;

    private final AnimationEventSink eventSink = pendingEvents::add;

    public AnimationController(AnimatedObject animatedObject) {
        this.object = animatedObject;
    }

    public record QueueEntry(
            String animationName, boolean loop, boolean hold,
            float transitionTimeSec, int layer, @Nullable Set<String> boneMask
    ) {}

    public void play(String animationName, boolean loop) {
        play(animationName, loop, false, 0.25f, 0, null);
    }
    public void play(String animationName, boolean loop, boolean hold) {
        play(animationName, loop, hold, 0.25f, 0, null);
    }
    public void play(String animationName, boolean loop, boolean hold, float transitionTimeSec, int layer, @Nullable Set<String> boneMask) {
        commands.add(() -> playInternal(animationName, loop, hold, transitionTimeSec, layer, boneMask));
    }
    public void queue(String animationName, boolean loop) {
        queue(animationName, loop, false, 0.25f, 0, null);
    }
    public void queue(String animationName, boolean loop, boolean hold) {
        queue(animationName, loop, hold, 0.25f, 0, null);
    }
    public void queue(String animationName, boolean loop, boolean hold, float transitionTimeSec, int layer, @Nullable Set<String> boneMask) {
        commands.add(() -> animationQueue.addLast(new QueueEntry(animationName, loop, hold, transitionTimeSec, layer, boneMask)));
    }
    public void clearQueue() { commands.add(animationQueue::clear); }
    public void pause(String animationName) {
        commands.add(() -> {
            AnimationTrack t = activeTracks.get(animationName);
            if(t!=null) t.setPaused(true);
        });
    }
    public void resume(String animationName) {
        commands.add(() -> {
            AnimationTrack t = activeTracks.get(animationName);
            if(t!=null) t.setPaused(false);
        });
    }
    public void stop(String animationName) {
        commands.add(() -> stopInternal(animationName));
    }
    public void stopAll() {
        commands.add(() -> {
            activeTracks.clear();
            animationQueue.clear();
            markDirty();
        });
    }
    public void addModifier(BoneModifier modifier) {
        if (modifier != null) boneModifiers.add(modifier);
    }
    public void removeModifier(BoneModifier modifier) {
        if (modifier != null) boneModifiers.remove(modifier);
    }
    public void clearModifiers() {
        boneModifiers.clear();
    }
    public Map<String, AnimationTrack> getActiveTracks() {
        return Map.copyOf(activeTracks);
    }
    public Queue<QueueEntry> getAnimationInQueue() {
        return new ArrayDeque<>(animationQueue);
    }

    public void drainEvents() {
        AnimationEvent event;
        while ((event = pendingEvents.poll()) != null) {
            try { event.run(); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    public void update(float deltaTime, List<BoneRData> rootBones, AnimatedObjectCache cache) {
        update(deltaTime, false, rootBones, cache);
    }

    public void update(float deltaTime, boolean gamePaused, List<BoneRData> rootBones, AnimatedObjectCache cache) {
        processCommands();
        removeStoppedTracks();

        if (!object.shouldPlayAnimationsWhileGamePaused() && gamePaused) return;
        if (rootBones == null || cache == null) return;

        if (rootBones.isEmpty()) {
            cache.prepareWrite();
            cache.publish();
            return;
        }

        boolean removed = false;
        Iterator<Map.Entry<String, AnimationTrack>> iterator = activeTracks.entrySet().iterator();
        while (iterator.hasNext()) {
            AnimationTrack track = iterator.next().getValue();
            track.update(deltaTime, eventSink);
            if (track.isStopped()) { iterator.remove(); removed = true; }
        }
        if (removed) markDirty();

        processQueue();
        rebuildSortedTracksIfDirty();

        cache.prepareWrite();
        Matrix4f parentWorldMatrix = new Matrix4f();
        Matrix4f parentRestWorldMatrix = new Matrix4f();

        for (BoneRData rootBone : rootBones) {
            transformBoneRecursively(rootBone, parentRestWorldMatrix, parentWorldMatrix, sortedTracksCache, cache);
        }
        cache.publish();
    }

    private void processCommands() {
        Runnable command;
        while ((command = commands.poll()) != null) {
            try { command.run(); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    private void removeStoppedTracks() {
        if (activeTracks.values().removeIf(AnimationTrack::isStopped)) markDirty();
    }

    private void processQueue() {
        QueueEntry next;
        while ((next = animationQueue.peekFirst()) != null) {
            if (!isLayerBusy(next.layer())) {
                animationQueue.pollFirst();
                playInternal(next.animationName(), next.loop(), next.hold(), next.transitionTimeSec(), next.layer(), next.boneMask());
            } else break;
        }
    }

    private boolean isLayerBusy(int layer) {
        for (AnimationTrack track : activeTracks.values()) {
            if (track.getLayer() == layer && !track.isStopped()) return true;
        }
        return false;
    }

    private void playInternal(String animationName, boolean loop, boolean hold, float transitionTimeSec, int layer, @Nullable Set<String> boneMask) {
        if (animationName == null) return;

        RuntimeAnimation runtimeAnimation = AnimationCache.getRuntimeAnimation(animationName);
        if (runtimeAnimation == null) return;

        AnimationTrack existing = activeTracks.get(animationName);
        if (existing != null) {
            existing.setLoop(loop);
            existing.setHoldOnLastFrame(hold);
            existing.setLayer(layer);
            existing.setBoneMask(boneMask);
            existing.fadeIn(transitionTimeSec);
            markDirty();
            return;
        }

        for (AnimationTrack track : activeTracks.values()) {
            if (track.getLayer() == layer && !track.isStopped() && !track.getAnimation().name().equals(animationName)) {
                track.fadeOut(transitionTimeSec);
            }
        }

        AnimationTrack newTrack = new AnimationTrack(runtimeAnimation);
        newTrack.setLoop(loop);
        newTrack.setHoldOnLastFrame(hold);
        newTrack.setLayer(layer);
        newTrack.setBoneMask(boneMask);
        newTrack.fadeIn(transitionTimeSec);

        activeTracks.put(animationName, newTrack);
        markDirty();
    }

    private void stopInternal(String animationName) {
        AnimationTrack track = activeTracks.remove(animationName);
        if (track != null) { track.stop(); markDirty(); }
    }

    private void markDirty() { tracksDirty = true; }

    private void rebuildSortedTracksIfDirty() {
        if (!tracksDirty) return;
        sortedTracksCache.clear();
        sortedTracksCache.addAll(activeTracks.values());
        sortedTracksCache.sort(Comparator.comparingInt(AnimationTrack::getLayer));
        tracksDirty = false;
    }

    private void transformBoneRecursively(BoneRData bone, Matrix4f parentRestWorldMatrix, Matrix4f parentWorldMatrix, List<AnimationTrack> tracks, AnimatedObjectCache cache) {
        String boneName = bone.name();
        BoneTransform finalTransform = new BoneTransform(bone);

        for (AnimationTrack track : tracks) {
            float weight = track.getBlendWeight();
            if (weight <= 0.0f) continue;
            if (weight > 1.0f) weight = 1.0f;
            if (track.getBoneMask() != null && !track.getBoneMask().contains(boneName)) continue;

            BoneAData boneAnimData = track.getRuntimeAnimation().getBone(boneName);

            if (boneAnimData != null) {
                BoneTransform trackTransform = calculateInterpolatedTransform(boneAnimData, track.getCurrentTime());
                finalTransform = BoneTransform.blend(finalTransform, trackTransform, weight);
            }
        }

        for (BoneModifier modifier : boneModifiers) {
            BoneTransform modified = modifier.apply(boneName, finalTransform);
            if (modified != null) finalTransform = modified;
        }

        Matrix4f staticLocalMatrix = new Matrix4f()
                .translation(bone.pivot()[0], bone.pivot()[1], bone.pivot()[2])
                .rotate(new Quaternionf(bone.rotation()[0], bone.rotation()[1], bone.rotation()[2], bone.rotation()[3]));

        float[] scale = bone.getScale();
        staticLocalMatrix.scale(scale[0], scale[1], scale[2]);

        Matrix4f boneLocalMatrix = finalTransform.buildMatrix();
        Matrix4f boneWorldMatrix = new Matrix4f(parentWorldMatrix).mul(boneLocalMatrix);
        Matrix4f boneRestWorldMatrix = new Matrix4f(parentRestWorldMatrix).mul(staticLocalMatrix);

        Matrix4f boneSkinMatrix = new Matrix4f(boneWorldMatrix).mul(new Matrix4f(boneRestWorldMatrix).invert());

        Vector3f headWorld = new Vector3f();
        boneWorldMatrix.getTranslation(headWorld);
        Vector3f tailWorld = new Vector3f();
        boneWorldMatrix.transformPosition(new Vector3f(bone.pivotEnd()[0], bone.pivotEnd()[1], bone.pivotEnd()[2]), tailWorld);

        cache.setBoneMatrices(boneName, boneLocalMatrix, boneWorldMatrix);
        cache.setBoneRestWorldMatrix(boneName, boneRestWorldMatrix);
        cache.setBoneSkinMatrix(boneName, boneSkinMatrix);
        cache.setBonePivots(boneName, headWorld, tailWorld);

        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                cache.setMeshMatrices(mesh, new Matrix4f(), boneWorldMatrix);
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                transformBoneRecursively(child, boneRestWorldMatrix, boneWorldMatrix, tracks, cache);
            }
        }
    }

    private BoneTransform calculateInterpolatedTransform(BoneAData boneAData, float currentTime) {
        List<Keyframe> keyframes = boneAData.keyframes();
        if (keyframes == null || keyframes.isEmpty()) return BoneTransform.identity();
        if (keyframes.size() == 1) {
            Keyframe only = keyframes.get(0);
            if (only == null || only.data() == null || only.data().transform() == null) return BoneTransform.identity();
            return new BoneTransform(only.data().transform());
        }

        Keyframe first = keyframes.get(0);
        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (first == null || last == null || first.data() == null || last.data() == null) return BoneTransform.identity();

        float firstStartSec = first.startValue() / 1000.0f;
        float lastEndSec = last.endValue() / 1000.0f;

        if (currentTime <= firstStartSec) return new BoneTransform(first.data().transform());
        if (currentTime >= lastEndSec) return new BoneTransform(last.data().transform());

        int currentIndex = findCurrentKeyframeIndex(keyframes, currentTime);
        Keyframe current = keyframes.get(currentIndex);
        if (current == null || current.data() == null) return BoneTransform.identity();

        Keyframe next = currentIndex + 1 < keyframes.size() ? keyframes.get(currentIndex + 1) : current;
        if (next == null || next.data() == null) return new BoneTransform(current.data().transform());

        float currentStartSec = current.startValue() / 1000.0f;
        float currentEndSec = current.endValue() / 1000.0f;
        float nextStartSec = next.startValue() / 1000.0f;

        if (current != next && currentTime >= currentEndSec && currentTime < nextStartSec) {
            return new BoneTransform(current.data().transform());
        }

        float duration = currentEndSec - currentStartSec;
        float alpha = 0.0f;
        if (duration > 0.0001f) {
            alpha = (currentTime - currentStartSec) / duration;
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }

        InterpolationType interpolation = current.data().interpolation() == null ? InterpolationType.LINEAR : current.data().interpolation();
        Easing easing = current.data().easing() == null ? Easing.AUTOMATIC : current.data().easing();

        switch (interpolation) {
            case STEP -> alpha = 0.0f;
            case BEZIER -> {
                List<Float> bezierArgs = current.data().bezierArgs();
                if (bezierArgs != null && bezierArgs.size() == 4) {
                    alpha = getBezierValueSafe(alpha, bezierArgs.get(0), bezierArgs.get(1), bezierArgs.get(2), bezierArgs.get(3));
                }
            }
            default -> alpha = EasingMath.apply(alpha, interpolation, easing);
        }

        if (alpha <= 0.0f) return new BoneTransform(current.data().transform());
        if (alpha >= 1.0f && interpolation != InterpolationType.BACK && interpolation != InterpolationType.ELASTIC) {
            return new BoneTransform(next.data().transform());
        }

        Transform prevTransform = current.data().transform();
        Transform nextTransform = next.data().transform();

        float posX = lerp(prevTransform.posX(), nextTransform.posX(), alpha);
        float posY = lerp(prevTransform.posY(), nextTransform.posY(), alpha);
        float posZ = lerp(prevTransform.posZ(), nextTransform.posZ(), alpha);
        float scaleX = lerp(prevTransform.scaleX(), nextTransform.scaleX(), alpha);
        float scaleY = lerp(prevTransform.scaleY(), nextTransform.scaleY(), alpha);
        float scaleZ = lerp(prevTransform.scaleZ(), nextTransform.scaleZ(), alpha);

        Quaternionf q0 = new Quaternionf(prevTransform.rotX(), prevTransform.rotY(), prevTransform.rotZ(), prevTransform.rotW());
        Quaternionf q1 = new Quaternionf(nextTransform.rotX(), nextTransform.rotY(), nextTransform.rotZ(), nextTransform.rotW());
        Quaternionf rotation = new Quaternionf(q0).slerp(q1, Math.max(0.0f, Math.min(1.0f, alpha)));

        return new BoneTransform(new Transform(posX, posY, posZ, rotation.x, rotation.y, rotation.z, rotation.w, scaleX, scaleY, scaleZ));
    }

    private static int findCurrentKeyframeIndex(List<Keyframe> keyframes, float timeSec) {
        int low = 0, high = keyframes.size() - 1, result = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (keyframes.get(mid).startValue() / 1000.0f <= timeSec) { result = mid; low = mid + 1; }
            else high = mid - 1;
        }
        return result;
    }

    private float getBezierValueSafe(float alpha, float x1, float y1, float x2, float y2) {
        if (alpha <= 0.0f) return 0.0f;
        if (alpha >= 1.0f) return 1.0f;
        float t = alpha; boolean converged = false;
        for (int i = 0; i < 8; i++) {
            float x = ((1 - t) * (1 - t) * (1 - t) * 0 + 3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t * 1) - alpha;
            float derivative = 3 * (1 - t) * (1 - t) * (x1 - 0) + 6 * (1 - t) * t * (x2 - x1) + 3 * t * t * (1 - x2);
            if (Math.abs(derivative) < 1e-5f) break;
            float nextT = t - x / derivative;
            if (Math.abs(nextT - t) < 1e-5f) { converged = true; t = nextT; break; }
            t = Math.max(0.0f, Math.min(1.0f, nextT));
        }
        if (!converged) return alpha;
        return 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t * 1;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}