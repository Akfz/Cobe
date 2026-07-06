package v.akfz.cobe.aengine.animation;

import v.akfz.cobe.aengine.data.bone.BoneRData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncAnimationEngine {
    private static final AsyncAnimationEngine INSTANCE = new AsyncAnimationEngine();
    public static AsyncAnimationEngine getInstance() { return INSTANCE; }

    private final Map<String, AnimatedObject> registry = new ConcurrentHashMap<>();

    private ThreadPoolExecutor workerPool;
    private Thread tickerThread;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private long targetFrameTimeNs;
    private int currentFps;

    private int coreThreads;
    private int maxThreads;

    private AsyncAnimationEngine() {
        setTargetFps(60);
    }

    public int getCurrentFps() {
        return currentFps;
    }

    public void setTargetFps(int fps) {
        if (fps <= 0) fps = 60;
        this.currentFps = fps;
        this.targetFrameTimeNs = 1_000_000_000L / fps;
    }

    public synchronized void start() {
        if (isRunning.get()) return;
        isRunning.set(true);

        coreThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        maxThreads = Math.max(4, Runtime.getRuntime().availableProcessors() + 2);

        workerPool = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                5L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread thread = new Thread(r, "Cobe-Animation-Worker");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        tickerThread = new Thread(this::runTicker, "Cobe-Animation-Ticker");
        tickerThread.setDaemon(true);
        tickerThread.start();
    }

    private void runTicker() {
        long lastTime = System.nanoTime();

        while (isRunning.get()) {
            long currentTime = System.nanoTime();
            long elapsedTimeNs = currentTime - lastTime;

            if (elapsedTimeNs >= targetFrameTimeNs) {
                lastTime = currentTime;

                float deltaTimeSec = (float) elapsedTimeNs / 1_000_000_000f;

                tickAllControllersAndWait(deltaTimeSec);

                long tickEndTime = System.nanoTime();
                long actualWorkTimeNs = tickEndTime - currentTime;

                if (actualWorkTimeNs > (targetFrameTimeNs * 0.9f)) {
                    int currentCore = workerPool.getCorePoolSize();
                    if (currentCore < maxThreads) {
                        workerPool.setCorePoolSize(Math.min(maxThreads, currentCore + 1));
                    }
                } else if (actualWorkTimeNs < (targetFrameTimeNs * 0.4f)) {
                    int currentCore = workerPool.getCorePoolSize();
                    if (currentCore > coreThreads) {
                        workerPool.setCorePoolSize(Math.max(coreThreads, currentCore - 1));
                    }
                }
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void tickAllControllersAndWait(float deltaTimeSec) {
        if (registry.isEmpty()) return;

        CountDownLatch latch = new CountDownLatch(registry.size());

        for (AnimatedObject animatedObject : registry.values()) {
            try {
                workerPool.submit(() -> {
                    try {
                        List<BoneRData> rootBones = animatedObject.getCache().getRootBones();
                        if (rootBones != null) {
                            AnimationController controller = animatedObject.getController();
                            controller.update(deltaTimeSec, rootBones, animatedObject.getCache());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (RejectedExecutionException e) {
                try {
                    List<BoneRData> rootBones = animatedObject.getCache().getRootBones();
                    if (rootBones != null) {
                        AnimationController controller = animatedObject.getController();
                        controller.update(deltaTimeSec, rootBones, animatedObject.getCache());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
        }

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void stop() {
        if (!isRunning.get()) return;
        isRunning.set(false);

        if (tickerThread != null) tickerThread.interrupt();
        if (workerPool != null) {
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(1, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
            }
        }
        registry.clear();
    }

    public void register(String uniqueId, AnimatedObject obj) {
        registry.put(uniqueId, obj);
    }

    public void unregister(String uniqueId) {
        registry.remove(uniqueId);
    }

    @Nullable
    public AnimatedObject getAnimatedObject(String uniqueId) {
        return registry.get(uniqueId);
    }
}