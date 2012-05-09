package h.vrp.search;

public class AnnealerThread implements Runnable {
	private Annealer annealer;
	private boolean running;
	public AnnealerThread(Annealer annealer) {
		this.annealer = annealer;
	}
	@Override
	public void run() {
		running = true;
		while (running) {
			annealer.step();
		}
	}
	
	public void stop() {
		running = false;
	}
}
