package v.akfz.cobe.nat;

import v.akfz.db.annotation.ProdOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ProdOnly //test
public final class NativeVideoPlayer extends JPanel implements Runnable {

	private static final String LIB_NAME = "cobe_native_decoder";

	private static final double SPEED_MIN     = 0.10;
	private static final double SPEED_MAX     = 8.00;
	private static final double SPEED_STEP    = 1.25;
	private static final double SPEED_DEFAULT = 1.00;

	private final NativeVideoDecoder decoder;
	private final BufferedImage image;

	private volatile boolean running = true;
	private volatile boolean paused  = false;
	private volatile boolean fitToWindow = true;
	private volatile int    currentFrame = 0;
	private volatile double speed = SPEED_DEFAULT;

	public NativeVideoPlayer(NativeVideoDecoder decoder) {
		this.decoder = decoder;
		this.image = new BufferedImage(
				decoder.getWidth(), decoder.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		setPreferredSize(new Dimension(decoder.getWidth(), decoder.getHeight()));
		setBackground(Color.BLACK);
		setFocusable(true);
	}

	public void stop() { running = false; }

	public void nudgeSpeed(double factor) {
		double s = speed * factor;
		if (s < SPEED_MIN) s = SPEED_MIN;
		if (s > SPEED_MAX) s = SPEED_MAX;
		speed = s;
	}

	public void resetSpeed() { speed = SPEED_DEFAULT; }

	@Override
	public void run() {
		final int frameCount = decoder.getFrameCount();
		final double baseFps = Math.max(1.0, decoder.getFps());

		final int[] dst = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

		long nextFrameTime = System.nanoTime();
		double lastSpeed = speed;

		while (running) {
			if (paused) {
				try { Thread.sleep(30); } catch (InterruptedException e) { return; }
				nextFrameTime = System.nanoTime();
				lastSpeed = speed;
				continue;
			}

			boolean ok = decoder.decodeFrame(currentFrame);
			if (!ok) {
				currentFrame = 0;
				nextFrameTime = System.nanoTime();
				continue;
			}

			ByteBuffer buf = decoder.frameBuffer();
			buf.rewind();
			IntBuffer src = buf.asIntBuffer();
			int n = Math.min(src.remaining(), dst.length);
			for (int i = 0; i < n; i++) {
				int v = src.get(i);
				dst[i] = ((v & 0xFF) << 16) | (v & 0xFF00) | ((v >> 16) & 0xFF);
			}

			SwingUtilities.invokeLater(this::repaint);

			currentFrame++;
			if (frameCount > 0 && currentFrame >= frameCount) currentFrame = 0;

			double spd = speed;
			long frameDelayNs = (long)(1_000_000_000.0 / (baseFps * spd));

			if (spd != lastSpeed) {
				nextFrameTime = System.nanoTime() + frameDelayNs;
				lastSpeed = spd;
			} else {
				nextFrameTime += frameDelayNs;
			}

			long sleepNs = nextFrameTime - System.nanoTime();
			while (sleepNs > 0 && running) {
				long chunk = Math.min(sleepNs, 20_000_000L);
				try {
					Thread.sleep(chunk / 1_000_000, (int)(chunk % 1_000_000));
				} catch (InterruptedException e) { return; }

				if (speed != lastSpeed) break;

				sleepNs = nextFrameTime - System.nanoTime();
			}

			if (nextFrameTime < System.nanoTime()) {
				nextFrameTime = System.nanoTime();
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int pw = getWidth(), ph = getHeight();
		int iw = image.getWidth(), ih = image.getHeight();

		int dw, dh, dx, dy;
		if (fitToWindow) {
			double scale = Math.min((double)pw / iw, (double)ph / ih);
			dw = (int)(iw * scale);
			dh = (int)(ih * scale);
			dx = (pw - dw) / 2;
			dy = (ph - dh) / 2;
		} else {
			dw = iw; dh = ih;
			dx = (pw - dw) / 2;
			dy = (ph - dh) / 2;
		}

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				fitToWindow
						? RenderingHints.VALUE_INTERPOLATION_BILINEAR
						: RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(image, dx, dy, dw, dh, null);

		String status = String.format(
				"frame %d / %d   %.1f fps   %dx%d   speed %.2fx%s",
				currentFrame, decoder.getFrameCount(),
				decoder.getFps(),
				iw, ih,
				speed,
				paused ? "   [PAUSED]" : "");

		g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
		FontMetrics fm = g2.getFontMetrics();
		int tw = fm.stringWidth(status);
		g2.setColor(new Color(0, 0, 0, 140));
		g2.fillRect(6, 4, tw + 12, fm.getHeight() + 2);
		g2.setColor(Color.WHITE);
		g2.drawString(status, 12, 4 + fm.getAscent());

		if (decoder.getFrameCount() > 0) {
			g2.setColor(new Color(255, 255, 255, 60));
			g2.fillRect(0, ph - 3, pw, 3);
			g2.setColor(new Color(255, 80, 80));
			int bar = (int)((double)currentFrame / decoder.getFrameCount() * pw);
			g2.fillRect(0, ph - 3, bar, 3);
		}

		g2.dispose();
	}

	public static void main(String[] args) throws Exception {
		if (!NativeLoader.load(LIB_NAME)) {
			System.err.println("[player] FAIL: " + NativeLoader.getLastError());
			System.exit(1);
		}

		//замени на свой файл (полный путь)
		Path video = Paths.get("/home/yaxzlol/Загрузки/giphy.gif");
		if (!Files.exists(video)) {
			System.err.println("file not found: " + video.toAbsolutePath());
			System.exit(2);
		}

		byte[] data = Files.readAllBytes(video);
		NativeVideoDecoder dec = new NativeVideoDecoder(data);

		System.out.printf("opened: %dx%d @ %.2f fps, %d frames%n",
				dec.getWidth(), dec.getHeight(),
				dec.getFps(), dec.getFrameCount());

		NativeVideoPlayer player = new NativeVideoPlayer(dec);

		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame("NativeVideoPlayer — " + video.getFileName());
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setContentPane(player);
			f.pack();
			f.setMinimumSize(new Dimension(320, 240));
			f.setLocationRelativeTo(null);
			f.setVisible(true);

			player.addKeyListener(new KeyAdapter() {
				@Override public void keyPressed(KeyEvent e) {
					switch (e.getKeyCode()) {
						case KeyEvent.VK_SPACE -> player.paused = !player.paused;
						case KeyEvent.VK_F     -> player.fitToWindow = !player.fitToWindow;

						case KeyEvent.VK_COMMA -> player.nudgeSpeed(1.0 / SPEED_STEP);
						case KeyEvent.VK_PERIOD -> player.nudgeSpeed(SPEED_STEP);
						case KeyEvent.VK_0      -> player.resetSpeed();

						case KeyEvent.VK_ESCAPE -> {
							player.stop();
							dec.close();
							f.dispose();
							System.exit(0);
						}
					}
				}
			});
			player.requestFocusInWindow();
		});

		Thread t = new Thread(player, "native-video-player");
		t.setDaemon(true);
		t.start();
	}

	private NativeVideoPlayer() {
		throw new AssertionError();
	}
}