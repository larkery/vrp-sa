package h.vrp.search.cl;

import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.Solution.IEdgeListener;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class CrossingTracker implements IEdgeListener, Iterable<Crossing> {
	HashSet<Crossing> crossings = new HashSet<Crossing>();
	SortedSet<Edge> edges = new TreeSet<Edge>();
	private List<Point2D.Float> points;
	Solution solution;
	
	public CrossingTracker(Instance instance, Solution solution) {
		solution.addEdgeListener(this);
		//force initialize
		points = instance.getPoints();
		for (Route r : solution) {
			for (int i = 0; i<r.size(); i++) {
				edgeAdded(solution, r.at(i), r.at(i+1));
			}
		}
	}

	@Override
	public void edgeAdded(Solution solution, int from, int to) {
		this.solution = solution;
		if (from == to) return;
		edges.add(new Edge(from, to));
		
		final Line2D.Float l = new Line2D.Float(points.get(from), points.get(to));
		
		for (Edge e: edges) {
			if (!e.meets(from, to)) {
				final Line2D.Float l2 = new Line2D.Float(points.get(e.getFrom()), points.get(e.getTo()));
				if (l.intersectsLine(l2)) {
					crossings.add(new Crossing(from, to, e.getFrom(), e.getTo()));
				}
			}
		}
	}

	@Override
	public void edgeDeleted(Solution solution, int from, int to) {
		if (from == to) return;
		edges.remove(new Edge(from, to));
		
		Iterator<Crossing> iter = crossings.iterator();
		while (iter.hasNext()) {
			final Crossing c = iter.next();
			if (c.containsEdge(from, to))
				iter.remove();
		}
	}

	public final class Edge implements Comparable<Edge> {
		final int from, to;

		public Edge(int from, int to) {
			super();
			this.from = Math.min(from, to);
			this.to = Math.max(from, to);
		}

		public final int getFrom() {
			return from;
		}

		public final int getTo() {
			return to;
		}

		public boolean meets(int from, int to) {
			return (from == this.from || from == this.to || to == this.from || to == this.to);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + from;
			result = prime * result + to;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge other = (Edge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (from != other.from)
				return false;
			if (to != other.to)
				return false;
			return true;
		}

		private CrossingTracker getOuterType() {
			return CrossingTracker.this;
		}

		@Override
		public int compareTo(Edge o) {
			if (o.from == from) {
				return to - o.to;
			}
			return from - o.from;
		}
	}

	@Override
	public Iterator<Crossing> iterator() {
		return crossings.iterator();
	}

	@Override
	public String toString() {
		return "CrossingTracker [crossings=" + crossings + ", edges=" + edges
				+ "]";
	}

	public int size() {
		return crossings.size();
	}

	@Override
	protected void finalize() throws Throwable {
		solution.removeEdgeListener(this);
		super.finalize();
	}
}
