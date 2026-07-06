package v.akfz.cobe.aengine.animation;

import v.akfz.cobe.loader.json.animation.Animation;
import org.jetbrains.annotations.Nullable;
import java.util.Set;

public class AnimationTrack {
    private final Animation animation;
    private boolean loop;
    private boolean isPaused = false;
    private boolean isStopped = false;
    private float currentTime = 0;

    private int layer = 0;
    private @Nullable Set<String> boneMask = null;
    private float blendWeight = 1.0f;
    private float fadeSpeed = 0.0f;

    private boolean holdOnLastFrame = false;

    public AnimationTrack(Animation animation) {
        this.animation = animation;
    }

    public void update(float deltaTimeSec) {
        if (isPaused || isStopped) return;

        this.currentTime += deltaTimeSec * animation.speed();

        if (fadeSpeed != 0.0f) {
            this.blendWeight += deltaTimeSec * fadeSpeed;
            if (fadeSpeed > 0.0f && this.blendWeight >= 1.0f) {
                this.blendWeight = 1.0f;
                this.fadeSpeed = 0.0f;
            } else if (fadeSpeed < 0.0f && this.blendWeight <= 0.0f) {
                this.blendWeight = 0.0f;
                this.fadeSpeed = 0.0f;
                this.isStopped = true;
            }
        }

        float lengthInSec = animation.length() / 1000f;

        if (this.currentTime >= lengthInSec) {
            if (loop) {
                this.currentTime = this.currentTime % lengthInSec;
            } else if (holdOnLastFrame) {
                this.currentTime = lengthInSec;
            } else {
                this.isStopped = true;
                this.currentTime = lengthInSec;
            }
        }
    }

    public void fadeIn(float fadeTimeSec) {
        if (fadeTimeSec <= 0.0f) {
            this.blendWeight = 1.0f;
            this.fadeSpeed = 0.0f;
        } else {
            this.fadeSpeed = 1.0f / fadeTimeSec;
        }
        this.isStopped = false;
    }

    public void fadeOut(float fadeTimeSec) {
        if (fadeTimeSec <= 0.0f) {
            this.blendWeight = 0.0f;
            this.fadeSpeed = 0.0f;
            this.isStopped = true;
        } else {
            this.fadeSpeed = -1.0f / fadeTimeSec;
        }
    }

    public void stop() {
        this.isStopped = true;
    }

    public void reset() {
        this.currentTime = 0.0f;
        this.isStopped = false;
        this.isPaused = false;
    }

    public Animation getAnimation() {
        return animation;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    @Nullable
    public Set<String> getBoneMask() {
        return boneMask;
    }

    public void setBoneMask(@Nullable Set<String> boneMask) {
        this.boneMask = boneMask;
    }

    public float getBlendWeight() {
        return blendWeight;
    }

    public boolean isHoldOnLastFrame() {
        return holdOnLastFrame;
    }

    public void setHoldOnLastFrame(boolean holdOnLastFrame) {
        this.holdOnLastFrame = holdOnLastFrame;
    }
}