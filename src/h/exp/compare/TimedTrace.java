package h.exp.compare;

import h.util.timing.Timer;
import h.vrp.model.Solution;
import h.vrp.search.IFitnessListener;
import h.vrp.search.IOptimiser;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimedTrace implements IFitnessListener {
	class Event {
		long ticks;
		float fitness;
		double time;
		public Event(long ticks, float fitness, double time) {
			super();
			this.ticks = ticks;
			this.fitness = fitness;
			this.time = time;
		}
		public String toString() {
			return "(" + ticks + ", " + time + ", " + fitness + ")";
		}
	}
	
	boolean started = false;
	Timer timer;
	List<Event> events = new ArrayList<Event>();
	private double minimumInterval;
	private double lastInterval;
	
	public TimedTrace(double minimumInterval) {
		this.minimumInterval = minimumInterval;
		timer = Timer.getInstance();
	}
	
	public TimedTrace() {
		this(0);
	}
	
	@Override
	public void fitnessChanged(IOptimiser source, Solution state, float newFitness, long ticks) {
		final double t = timer.getElapsedTime();
		
		final Event e = new Event(ticks, newFitness, t);
//		System.err.println("Event: " + e);
		if (!started) {
			lastInterval = 0;
			started = true;
			timer.reset();
			events.add(e);
		} else {
			if (t < lastInterval + minimumInterval) {
				//scrub
				events.set(events.size()-1, e);
			} else {
				events.add(e);
				lastInterval = t;
			}
		}
	}

	public String toString() {
		return events.toString();
	}
	
	public Iterator<Point2D.Double> timeIterator() {
		final Iterator<Event> iter = events.iterator();
		return new Iterator<Point2D.Double>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Double next() {
				Event e = iter.next();
				return new Point2D.Double(e.time, e.fitness);
			}

			@Override
			public void remove() {
				iter.remove();
			}
		};
	}
	
	public Iterator<Point2D.Double> tickIterator() {
		final Iterator<Event> iter = events.iterator();
		return new Iterator<Point2D.Double>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Double next() {
				Event e = iter.next();
				return new Point2D.Double(e.ticks, e.fitness);
			}

			@Override
			public void remove() {
				iter.remove();
			}
		};
	}
	
	public double lastTime() {
		return events.get(events.size()-1).time;
	}
	
	public double lastTick() {
		return events.get(events.size()-1).ticks;
	}
}
