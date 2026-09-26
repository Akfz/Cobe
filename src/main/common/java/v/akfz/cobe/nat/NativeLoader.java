package v.akfz.cobe.nat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class NativeLoader {

	private static boolean loaded = false;
	private static String lastError = null;

	private NativeLoader() {}

	public static boolean load(String libraryName) {
		if (loaded) return true;
		try {
			loadLibrary(libraryName);
			loaded = true;
			return true;
		} catch (Exception e) {
			lastError = e.getMessage();
			System.err.println("[JNIHELPER] Failed to load native library '" + libraryName + "': " + lastError);
			return false;
		}
	}

	public static boolean isLoaded() { return loaded; }
	public static String getLastError() { return lastError; }

	private static void loadLibrary(String libraryName) throws Exception {
		String osName = System.getProperty("os.name").toLowerCase();
		String archName = System.getProperty("os.arch").toLowerCase();

		String osId = detectOsId(osName);
		String archId = detectArchId(archName);
		String ext = getExtension(osId);
		String prefix = getPrefix(osId);

		String fileName = prefix + libraryName + "." + ext;
		String resourcePath = "/native/" + osId + "-" + archId + "/" + fileName;

		InputStream in = NativeLoader.class.getResourceAsStream(resourcePath);
		if (in == null) {
			throw new RuntimeException(
					"Native library resource not found: " + resourcePath +
							" (detected platform: " + osId + "-" + archId + ")"
			);
		}

		File tempFile = File.createTempFile(libraryName + "_", "." + ext);
		tempFile.deleteOnExit();
		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		in.close();

		tempFile.setExecutable(true);
		System.load(tempFile.getAbsolutePath());
	}

	private static String detectOsId(String osName) {
		if (osName.contains("win")) return "windows";
		if (osName.contains("mac") || osName.contains("darwin")) return "macos";
		if (osName.contains("freebsd")) return "freebsd";
		if (osName.contains("nux") || osName.contains("nix")) {
			return Files.exists(Paths.get("/etc/alpine-release")) ? "linux-musl" : "linux";
		}
		throw new UnsupportedOperationException("Unsupported OS: " + osName);
	}

	private static String detectArchId(String arch) {
		if (arch.equals("amd64") || arch.equals("x86_64")) return "x86_64";
		if (arch.equals("aarch64") || arch.equals("arm64")) return "arm64";
		if (arch.equals("x86") || arch.equals("i386") || arch.equals("i486") ||
				arch.equals("i586") || arch.equals("i686")) return "x86";
		if (arch.equals("riscv64")) return "riscv64";
		if (arch.equals("ppc64le")) return "ppc64le";
		throw new UnsupportedOperationException("Unsupported architecture: " + arch);
	}

	private static String getExtension(String osId) {
		return switch (osId) {
			case "windows" -> "dll";
			case "macos" -> "dylib";
			default -> "so";
		};
	}

	private static String getPrefix(String osId) {
		return "windows".equals(osId) ? "" : "lib";
	}
}