package v.akfz.cobe.nat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class NativeVideoDecoder implements AutoCloseable {

	private long handle;
	private final ByteBuffer frameBuffer;
	private final int width;
	private final int height;
	private final double fps;
	private final int frameCount;

	public NativeVideoDecoder(byte[] fileData) {
		long h = open(fileData);
		if (h == 0) {
			throw new IllegalArgumentException(
					"NativeVideoDecoder: unsupported or corrupted video data");
		}
		this.handle     = h;
		this.width      = getWidth(h);
		this.height     = getHeight(h);
		this.fps        = getFps(h);
		this.frameCount = getFrameCount(h);

		this.frameBuffer = ByteBuffer
				.allocateDirect(width * height * 4)
				.order(ByteOrder.nativeOrder());
	}

	public boolean decodeFrame(int index) {
		checkOpen();
		frameBuffer.clear();
		return decodeFrame(handle, index, frameBuffer);
	}

	public ByteBuffer frameBuffer() { return frameBuffer; }
	public int getWidth()     { return width; }
	public int getHeight()    { return height; }
	public double getFps()    { return fps; }
	public int getFrameCount(){ return frameCount; }

	@Override
	public void close() {
		if (handle != 0) {
			close(handle);
			handle = 0;
		}
	}

	private void checkOpen() {
		if (handle == 0) throw new IllegalStateException("Decoder is closed");
	}

	private static native long    open(byte[] data);
	private static native int     getWidth(long h);
	private static native int     getHeight(long h);
	private static native double  getFps(long h);
	private static native int     getFrameCount(long h);
	private static native boolean decodeFrame(long h, int index, ByteBuffer out);
	private static native void    close(long h);
}