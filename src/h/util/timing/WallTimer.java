package h.util.timing;

/**
 * Lame fallback timer which uses {@link System.getCurrentTimeMillis()}, and so doesn't measure CPU time
 * @author hinton
 */
public class WallTimer extends Timer {
	@Override
	protected double getCurrentTime() {
		return System.currentTimeMillis() / 1000.0;
	}
}
