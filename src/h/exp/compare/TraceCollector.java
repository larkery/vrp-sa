package h.exp.compare;

import h.util.timing.Timer;
import h.vrp.search.IOptimiser;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TraceCollector {
	private List<TimedTrace> traces = new ArrayList<TimedTrace>();

	public TraceCollector() {}
	
	public void collectTrace(IOptimiser optimiser) {
		//half second resolution?
		TimedTrace trace = new TimedTrace(0.5);
		
		optimiser.setFitnessListener(trace);

		Timer t = Timer.getInstance();
		t.reset();
		
		while (!optimiser.finished()) {
			optimiser.step();
		}
		
//		while (true) {
//			if (t.getElapsedTime() > runtime) {
//				runner.stop();
//				break;
//			}
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {}
//		}
		optimiser.setFitnessListener(trace);
		optimiser.setFitnessListener(null);
		traces.add(trace);
	}
	
	

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (TimedTrace tr : traces) {
			sb.append(tr.toString());
			sb.append("\n");
		}
		return sb.toString();
//		return traces.toString();
	}
	
	public List<Double> processTimeSequence(int samples, TraceProcessor.IReducer reducer) {
		double maxTime = 0;
		List<Iterator<Point2D.Double>>iters = new ArrayList<Iterator<Point2D.Double>>();
		for (TimedTrace tt:traces) {
			maxTime = Math.max(maxTime, tt.lastTime());
			iters.add(tt.timeIterator());
		}
		return TraceProcessor.processTraces(iters, samples, maxTime, reducer);
	}
	
	public List<Double> processTickSequence(int samples, TraceProcessor.IReducer reducer) {
		double maxTick = 0;
		List<Iterator<Point2D.Double>>iters = new ArrayList<Iterator<Point2D.Double>>();
		for (TimedTrace tt:traces) {
			maxTick = Math.max(maxTick, tt.lastTick());
			iters.add(tt.tickIterator());
		}
		return TraceProcessor.processTraces(iters, samples, maxTick, reducer);
	}

	public List<List<Double>> rawTimeSequences(int i) {
		List<List<Double>> rv = new ArrayList<List<Double>>();
		double tmax = 0;
		List<Interpolator> interpolators = new ArrayList<Interpolator>();
		for (TimedTrace tr : traces) {
			rv.add(new ArrayList<Double>());
			tmax = Math.max(tr.lastTime(), tmax);
			interpolators.add(new Interpolator(tr.timeIterator()));
		}
		double pt = 0;
		double acc = tmax / i;
		while (i>0) {
			for (int j = 0; j<rv.size(); j++) {
				rv.get(j).add(
						new Double(pt, interpolators.get(j).valueAt(pt))
								);
			}
			i--;
			pt+=acc;
		}
		return rv;
	}

	public List<List<Double>> rawTickSequences(int i) {
		List<List<Double>> rv = new ArrayList<List<Double>>();
		double tmax = 0;
		List<Interpolator> interpolators = new ArrayList<Interpolator>();
		for (TimedTrace tr : traces) {
			rv.add(new ArrayList<Double>());
			tmax = Math.max(tr.lastTick(), tmax);
			interpolators.add(new Interpolator(tr.tickIterator()));
		}
		double pt = 0;
		double acc = tmax / i;
		while (i>0) {
			for (int j = 0; j<rv.size(); j++) {
				rv.get(j).add(
						new Double(pt, interpolators.get(j).valueAt(pt))
								);
			}
			i--;
			pt+=acc;
		}
		return rv;
	}
}
