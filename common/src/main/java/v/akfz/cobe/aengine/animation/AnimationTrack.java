package v.akfz.cobe.aengine.animation;

import v.akfz.cobe.json.animation.Animation;

public class AnimationTrack {
    private final Animation animation;
    private boolean loop;
    private boolean isPaused = false;
    private boolean isStopped = false;
    private float currentTime = 0; // Время накапливается в секундах

    public AnimationTrack(Animation animation) {
        this.animation = animation;
    }

    public void update(float deltaTimeMs) {
        if (isPaused || isStopped) return;

        float deltaTimeSec = deltaTimeMs / 1000f;
        this.currentTime += deltaTimeSec * animation.speed();

        float lengthInSec = animation.length() / 1000f;

        if (this.currentTime >= lengthInSec) {
            if (loop) {
                this.currentTime = this.currentTime % lengthInSec;
            } else {
                this.isStopped = true;
                this.currentTime = lengthInSec;
            }
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
}