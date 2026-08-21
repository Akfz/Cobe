package v.akfz.cobe.aengine.animation.event;

public interface AnimationEvent {
    void run();
    long time(); //писать в мс!
}
