package h.util.timing;

import java.lang.reflect.Constructor;

public abstract class Timer {
	double startTime;
	
	protected static Constructor<? extends Timer> constructor = null;
	
	protected abstract double getCurrentTime();
	public void reset() {
		startTime = getCurrentTime();
	}
	/**
	 * @return the elapsed time in seconds
	 */
	public double getElapsedTime() {
		return getCurrentTime() - startTime;
	}
	
	public static Timer getInstance() {
		if (constructor == null) {
			//attempt setup
			if (RealTimer.loadLibrary()) {
				try {
					constructor = RealTimer.class.getConstructor();
				} catch (Exception e) {}
			} else {
				try {
					constructor = WallTimer.class.getConstructor();
				} catch (Exception e) {}
			}
		}
		try {
			return constructor.newInstance();
		} catch (Exception e) {
			return new WallTimer();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		Timer t = getInstance();
		System.out.println(t.getClass());
		t.reset();
		for (int i = 0; i<10000; i++) {
			int j = i*i;
			System.out.println(j);
		}
		System.out.println(t.getElapsedTime());
	}
}
