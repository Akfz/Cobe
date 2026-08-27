package v.akfz.cobe.core.animation;

import org.jetbrains.annotations.Nullable;
import v.akfz.cobe.core.cache.AnimationCache;
import v.akfz.cobe.core.data.Animation;
import v.akfz.cobe.core.event.AnimationEvent;
import v.akfz.cobe.core.event.AnimationEventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class AnimationTrack {
    private final RuntimeAnimation runtimeAnimation;

    private boolean loop;
    private boolean isPaused = false;
    private boolean isStopped = false;

    private float rawTime = 0;
    private float currentTime = 0;
    private long lastEventMs = -1L;

    private int layer = 0;
    private @Nullable Set<String> boneMask = null;
    private float blendWeight = 1.0f;
    private float fadeSpeed = 0.0f;
    private boolean holdOnLastFrame = false;

    private final TreeMap<Long, List<AnimationEvent>> eventList = new TreeMap<>();

    public AnimationTrack(RuntimeAnimation runtimeAnimation) {
        if (runtimeAnimation == null) {
            throw new IllegalArgumentException("RuntimeAnimation cannot be null");
        }
        this.runtimeAnimation = runtimeAnimation;

        List<AnimationEvent> events = AnimationCache.getAnimationEvents(runtimeAnimation.getAnimation().name());
        if (events != null && !events.isEmpty()) {
            for (AnimationEvent event : events) {
                eventList.computeIfAbsent(event.time(), k -> new ArrayList<>()).add(event);
            }
        }
    }

    public RuntimeAnimation getRuntimeAnimation() {
        return runtimeAnimation;
    }

    public Animation getAnimation() {
        return runtimeAnimation.getAnimation();
    }

    public void update(float deltaTimeSec, AnimationEventSink sink) {
        if (isPaused || isStopped) return;
        if (sink == null) sink = event -> {};

        Animation animation = runtimeAnimation.getAnimation();
        float lastTimeSec = this.currentTime;

        this.rawTime += deltaTimeSec * animation.speed();

        if (this.rawTime < 0.0f) this.rawTime = 0.0f;

        if (fadeSpeed != 0.0f) {
            this.blendWeight += deltaTimeSec * fadeSpeed;
            if (fadeSpeed > 0.0f && this.blendWeight >= 1.0f) {
                this.blendWeight = 1.0f; this.fadeSpeed = 0.0f;
            } else if (fadeSpeed < 0.0f && this.blendWeight <= 0.0f) {
                this.blendWeight = 0.0f; this.fadeSpeed = 0.0f; this.isStopped = true;
            }
        }

        long lengthInMs = animation.length();
        if (lengthInMs <= 0L) {
            this.currentTime = 0.0f;
            this.rawTime = 0.0f;
            this.isStopped = true;
            return;
        }

        float lengthInSec = lengthInMs / 1000f;

        boolean wrapped = false;

        if (this.rawTime >= lengthInSec) {
            if (loop) {
                this.rawTime %= lengthInSec;
                this.currentTime %= lengthInSec;
                wrapped = true;
            } else {
                this.rawTime = lengthInSec;
            }
        }

        stepToRaw();

        long lastMs = (long) (lastTimeSec * 1000f);
        long currentMs = (long) (this.currentTime * 1000f);

        if (!loop && this.currentTime >= lengthInSec) {
            dispatchEventsInRange(lastMs, lengthInMs, sink);
            this.currentTime = lengthInSec;
            this.lastEventMs = lengthInMs;
            if (!holdOnLastFrame) this.isStopped = true;
        } else if (wrapped) {
            dispatchEventsInRange(lastMs, lengthInMs, sink);
            if (currentMs > 0) dispatchEventsInRange(-1L, currentMs, sink);
            this.lastEventMs = currentMs;
        } else {
            dispatchEventsInRange(lastMs, currentMs, sink);
            this.lastEventMs = currentMs;
        }
    }

    private void stepToRaw() {
        float fps = runtimeAnimation.getFpsAt(this.currentTime);

        if (fps <= 0.0f) {
            this.currentTime = this.rawTime;
            return;
        }

        int guard = 0;
        while (guard++ < 4096) {
            float step = 1.0f / runtimeAnimation.getFpsAt(this.currentTime);
            if (step <= 0.0f) break;
            if (this.currentTime + step > this.rawTime) break;
            this.currentTime += step;
        }

        if (guard >= 4096) {
            this.currentTime = this.rawTime;
            return;
        }

        if (this.currentTime > this.rawTime) {
            this.currentTime = this.rawTime;
        }
    }

    private void dispatchEventsInRange(long fromExclusiveMs, long toInclusiveMs, AnimationEventSink sink) {
        if (eventList.isEmpty() || toInclusiveMs <= fromExclusiveMs) return;
        SortedMap<Long, List<AnimationEvent>> triggered = eventList.subMap(fromExclusiveMs, false, toInclusiveMs, true);
        for (List<AnimationEvent> events : triggered.values()) {
            for (AnimationEvent event : events) sink.onEvent(event);
        }
    }

    public void fadeIn(float fadeTimeSec) {
        if (isStopped) { reset(); this.blendWeight = 0.0f; }
        if (fadeTimeSec <= 0.0f) { this.blendWeight = 1.0f; this.fadeSpeed = 0.0f; }
        else { this.fadeSpeed = 1.0f / fadeTimeSec; }
        this.isStopped = false;
    }

    public void fadeOut(float fadeTimeSec) {
        if (fadeTimeSec <= 0.0f) { this.blendWeight = 0.0f; this.fadeSpeed = 0.0f; this.isStopped = true; return; }
        if (this.blendWeight <= 0.0f) { this.blendWeight = 0.0f; this.fadeSpeed = 0.0f; this.isStopped = true; return; }
        this.fadeSpeed = -1.0f / fadeTimeSec;
    }

    public void stop() { this.isStopped = true; }

    public void reset() {
        this.rawTime = 0.0f;
        this.currentTime = 0.0f;
        this.lastEventMs = -1L;
        this.isStopped = false;
        this.isPaused = false;
    }

    public boolean isLoop() { return loop; }
    public void setLoop(boolean loop) { this.loop = loop; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public boolean isStopped() { return isStopped; }
    public float getCurrentTime() { return currentTime; }
    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }
    @Nullable public Set<String> getBoneMask() { return boneMask; }
    public void setBoneMask(@Nullable Set<String> boneMask) { this.boneMask = boneMask; }
    public float getBlendWeight() { return blendWeight; }
    public boolean isHoldOnLastFrame() { return holdOnLastFrame; }
    public void setHoldOnLastFrame(boolean holdOnLastFrame) { this.holdOnLastFrame = holdOnLastFrame; }
}