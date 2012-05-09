package h.util.timing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RealTimer extends Timer {
	static final String libName = "libcputime";
	native double[] getrusage(int mode);
	@Override
	protected double getCurrentTime() {
		double[] d = getrusage(0);
		return d[0] + d[1];
	}

	public static boolean loadLibrary() {
		RealTimer rt = new RealTimer();
		return rt.tryLoadLibrary();
	}
	boolean tryNativeCall() {
		try {
			@SuppressWarnings("unused")
			double[] usage = getrusage(0);
			return true;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
	}
	boolean tryLoadLibrary() {
		String extension = getLibraryExtension();
		if (extension != null) {
			InputStream is = getClass().getResourceAsStream(libName + "." + extension);
			if (is != null) {
				try {
					File temp = File.createTempFile(libName, "." + extension);
					OutputStream o = new FileOutputStream(temp);
					int i;
					while (-1 != (i = is.read())) o.write(i);
					o.close();
					System.load(temp.getAbsolutePath());
					return tryNativeCall();
				} catch (IOException e) {}
			}
		}
		return trySystemLoadLib();
		
	}
	boolean trySystemLoadLib() {
		System.loadLibrary(libName);
		return tryNativeCall();
	}
	
	static String getLibraryExtension() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("mac") >= 0) return "jnilib";
		else if (os.indexOf("nux") >= 0 || os.indexOf("nix") >= 0) return "so";
		else return null;
	}
}
