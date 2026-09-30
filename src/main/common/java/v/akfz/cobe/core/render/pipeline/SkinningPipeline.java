package v.akfz.cobe.core.render.pipeline;

import v.akfz.cobe.core.cache.AnimatedObjectCache;
import v.akfz.cobe.core.data.MeshRData;

//TODO
public final class SkinningPipeline {

	public enum Mode {
		CPU_ASYNC,
		GPU_SHADER
	}

	private static volatile Mode currentMode = Mode.CPU_ASYNC;
	private static GpuSkinningHandler gpuHandler = null;

	@FunctionalInterface
	public interface GpuSkinningHandler {
		void uploadAndBind(AnimatedObjectCache cache, MeshRData mesh);
	}

	public static Mode getMode() {
		return currentMode;
	}

	public static void setMode(Mode mode) {
		currentMode = mode;
	}

	public static void setGpuHandler(GpuSkinningHandler handler) {
		gpuHandler = handler;
	}

	public static boolean isGpuSkinning() {
		return currentMode == Mode.GPU_SHADER;
	}

	public static void prepareMeshSkinning(AnimatedObjectCache cache, MeshRData mesh) {
		if (currentMode == Mode.GPU_SHADER && gpuHandler != null && cache != null) {
			gpuHandler.uploadAndBind(cache, mesh);
		}
	}
}