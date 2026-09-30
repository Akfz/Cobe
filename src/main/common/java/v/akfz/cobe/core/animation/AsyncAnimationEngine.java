package v.akfz.cobe.core.animation;

import org.joml.Matrix4f;
import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.data.MeshRData;
import v.akfz.cobe.core.data.bone.BoneRData;
import v.akfz.cobe.core.object.AnimatedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class AsyncAnimationEngine {

    private static final AsyncAnimationEngine INSTANCE = new AsyncAnimationEngine();

    public static AsyncAnimationEngine getInstance() {
        return INSTANCE;
    }

    private final Map<String, AnimatedObject> registry = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<CountDownLatch> activeFrame = new AtomicReference<>();

    private volatile long targetFrameTimeNs = 16_666_666L;
    private volatile int workerCount = 2;
    private volatile boolean gamePaused = false;

    private ThreadPoolExecutor workerPool;
    private Thread tickerThread;

    private AsyncAnimationEngine() {
        setTargetFps(60);
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        int cores = Runtime.getRuntime().availableProcessors();
        workerCount = Math.max(2, cores - 1);

        AtomicInteger threadNumber = new AtomicInteger();

        workerPool = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(16, workerCount * 8)),
                r -> {
                    Thread thread = new Thread(
                            r,
                            "Cobe-Animation-Worker-" + threadNumber.incrementAndGet()
                    );
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        tickerThread = new Thread(this::ticker, "Cobe-Animation-Ticker");
        tickerThread.setDaemon(true);
        tickerThread.start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (tickerThread != null) {
            tickerThread.interrupt();
        }

        if (workerPool != null) {
            workerPool.shutdownNow();
        }

        try {
            if (tickerThread != null) {
                tickerThread.join(1000);
            }

            if (workerPool != null) {
                workerPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        activeFrame.set(null);
        registry.clear();
    }

    public void clearRegistry() {
        registry.clear();
    }

    public void register(String uniqueId, AnimatedObject obj) {
        if (uniqueId == null || obj == null) {
            return;
        }
        registry.put(uniqueId, obj);
    }

    public void unregister(String uniqueId) {
        if (uniqueId == null) {
            return;
        }
        registry.remove(uniqueId);
    }

    public void setTargetFps(int fps) {
        fps = Math.max(1, Math.min(240, fps));
        targetFrameTimeNs = Math.max(1L, 1_000_000_000L / fps);
    }

    public void setGamePaused(boolean paused) {
        this.gamePaused = paused;
    }

    public boolean isRunning() {
        return running.get();
    }

    private void ticker() {
        long lastTime = System.nanoTime();

        while (running.get()) {
            try {
                long frameTimeNs = targetFrameTimeNs;
                long now = System.nanoTime();
                long elapsedNs = now - lastTime;

                if (elapsedNs < 0) {
                    elapsedNs = 0;
                }

                if (elapsedNs >= frameTimeNs) {
                    lastTime = now;

                    float deltaTimeSec = elapsedNs / 1_000_000_000f;

                    if (deltaTimeSec > 0.1f) {
                        deltaTimeSec = 0.1f;
                    }

                    tickFrame(deltaTimeSec);
                } else {
                    long sleepNs = frameTimeNs - elapsedNs;

                    if (sleepNs > 1_000_000L) {
                        LockSupport.parkNanos(sleepNs - 500_000L);
                    } else {
                        LockSupport.parkNanos(100_000L);
                    }
                }
            } catch (Throwable t) {
                System.err.println("[Cobe-Engine] Ошибка в тикере");
                t.printStackTrace();
                LockSupport.parkNanos(10_000_000L);
            }
        }
    }

    private void tickFrame(float deltaTimeSec) {
        CountDownLatch previous = activeFrame.get();

        if (previous != null) {
            if (previous.getCount() > 0) {
                return;
            }

            activeFrame.compareAndSet(previous, null);
        }

        if (registry.isEmpty()) {
            return;
        }

        List<Map.Entry<String, AnimatedObject>> snapshot = new ArrayList<>(registry.entrySet());

        if (snapshot.isEmpty()) {
            return;
        }

        int workers = workerPool == null ? workerCount : workerPool.getCorePoolSize();

        int maxChunks = workers * 4;
        int chunkCount = Math.min(snapshot.size(), maxChunks);

        if (chunkCount <= 0) {
            return;
        }

        int chunkSize = (snapshot.size() + chunkCount - 1) / chunkCount;

        CountDownLatch latch = new CountDownLatch(chunkCount);

        if (!activeFrame.compareAndSet(null, latch)) {
            return;
        }

        for (int i = 0; i < chunkCount; i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, snapshot.size());

            List<Map.Entry<String, AnimatedObject>> chunk =
                    new ArrayList<>(snapshot.subList(from, to));

            try {
                workerPool.execute(() -> {
                    try {
                        for (Map.Entry<String, AnimatedObject> entry : chunk) {
                            updateObject(
                                    entry.getKey(),
                                    entry.getValue(),
                                    deltaTimeSec
                            );
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (Throwable t) {
                latch.countDown();
            }
        }

        try {
            long timeoutNs = Math.max(targetFrameTimeNs * 4L, 100_000_000L);
            boolean finished = latch.await(timeoutNs, TimeUnit.NANOSECONDS);

            if (finished) {
                activeFrame.compareAndSet(latch, null);
            } else {
                System.out.println(
                        "[Cobe-Engine] Кадр не успел обновиться. " +
                                "Следующие кадры пропускаются до завершения текущего."
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateObject(String id, AnimatedObject animatedObject, float deltaTimeSec) {
        try {
            if (animatedObject == null) return;

            AnimatedObjectCache cache = animatedObject.getCache();
            if (cache == null) return;

            List<BoneRData> rootBones = cache.getRootBones();
            if (rootBones == null || rootBones.isEmpty()) return;

            animatedObject.getController().update(
                    deltaTimeSec,
                    gamePaused,
                    rootBones,
                    cache
            );

            performAsyncSkinning(rootBones, cache);
        } catch (Exception e) {
            System.err.println("[Cobe-Engine] Ошибка при обновлении анимации " + id);
            e.printStackTrace();
        }
    }

    private void performAsyncSkinning(List<BoneRData> rootBones, AnimatedObjectCache cache) {
        Matrix4f[] skinMats = cache.getWriteIndexedSkinMatrices();
        for (BoneRData root : rootBones) {
            skinBoneRecursively(root, cache, skinMats);
        }
    }

    private void skinBoneRecursively(BoneRData bone, AnimatedObjectCache cache, Matrix4f[] skinMats) {
        if (bone.meshes() != null) {
            for (MeshRData mesh : bone.meshes()) {
                if (!mesh.isSkinned() || mesh.skinningData() == null) continue;

                List<float[]> restVertices = mesh.vertices();
                int vCount = restVertices.size();
                if (vCount == 0) continue;

                float[] out = cache.getOrCreateWriteSkinnedBuffer(mesh, vCount * 3);
                List<MeshRData.SkinningData> skins = mesh.skinningData();
                int skinsSize = skins.size();

                for (int i = 0; i < vCount; i++) {
                    float[] v = restVertices.get(i);
                    int idx = i * 3;
                    if (i >= skinsSize) {
                        out[idx]     = v[0];
                        out[idx + 1] = v[1];
                        out[idx + 2] = v[2];
                        continue;
                    }

                    MeshRData.SkinningData skin = skins.get(i);
                    float px = v[0], py = v[1], pz = v[2];
                    float sx = 0, sy = 0, sz = 0;
                    boolean hasValidWeight = false;

                    float[] weights = skin.weights();
                    int[] jointIndices = skin.jointIndices();
                    String[] joints = skin.joints();
                    int limit = Math.min(4, weights.length);

                    for (int j = 0; j < limit; j++) {
                        float w = weights[j];
                        if (w <= 0.0f) continue;

                        int jointId = -1;
                        if (jointIndices != null && j < jointIndices.length) {
                            jointId = jointIndices[j];
                        }
                        if (jointId < 0 && joints != null && j < joints.length && joints[j] != null) {
                            jointId = cache.getOrRegisterBoneId(joints[j]);
                            if (jointIndices != null && j < jointIndices.length) {
                                jointIndices[j] = jointId;
                            }
                        }

                        if (jointId < 0 || jointId >= skinMats.length) continue;
                        Matrix4f skinMat = skinMats[jointId];
                        if (skinMat == null) continue;

                        float tx = skinMat.m00() * px + skinMat.m10() * py + skinMat.m20() * pz + skinMat.m30();
                        float ty = skinMat.m01() * px + skinMat.m11() * py + skinMat.m21() * pz + skinMat.m31();
                        float tz = skinMat.m02() * px + skinMat.m12() * py + skinMat.m22() * pz + skinMat.m32();

                        sx += tx * w;
                        sy += ty * w;
                        sz += tz * w;
                        hasValidWeight = true;
                    }

                    if (hasValidWeight) {
                        out[idx]     = sx;
                        out[idx + 1] = sy;
                        out[idx + 2] = sz;
                    } else {
                        out[idx]     = px;
                        out[idx + 1] = py;
                        out[idx + 2] = pz;
                    }
                }
            }
        }

        if (bone.children() != null) {
            for (BoneRData child : bone.children()) {
                skinBoneRecursively(child, cache, skinMats);
            }
        }
    }
}