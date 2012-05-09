package h.vrp.model;

import h.defines.Defines;
import h.vrp.model.IRouteModification.IEdgeIterator;
import h.vrp.search.IMove;
import h.vrp.search.cl.noptk.RecombiningMove;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** 
 * Represents a solution as a list of {@link Route}s 
 */
public class Solution extends ArrayList<Route> implements Cloneable {
	private static final long serialVersionUID = 4461311289315437018L;
	
	@Override
	public Object clone() {
		Solution clone = new Solution(size(), routeForVertex.length);
		
		for (Route r : this) {
			Route r2 = clone.get(r.index);
			r2.addAll(r);
		}
		
		clone.initialize();
		
		return clone;
	}

	/**
	 * The ith element contains the index of the route containing vertex i
	 */
	int[] routeForVertex;
	/**
	 * The ith element contains the position within its route of vertex i; see also
	 * {@link routeForVertex}. Externally accessible via some getters and setters.
	 */
	int[] positionForVertex;
	
	/**
	 * element i, j is true iff the solution contains an edge from i to j
	 */
	boolean[][] containsEdge;
	
	/**
	 * Construct a solution with the given number of routes
	 * @param vehicleCount - how many routes 07853898643
	 */
	public Solution(int vehicleCount, int vertexCount) {
		super(vehicleCount);
		for (int i = 0; i<vehicleCount; i++) {
			add(new Route(i));
		}
		containsEdge = new boolean[vertexCount][vertexCount];
		positionForVertex = new int[vertexCount];
		routeForVertex = new int[vertexCount];
	}
	
	/**
	 * Re-make all the housekeeping structures and generally try and fix everything.
	 */
	public void initialize() {
		for (boolean[] a : containsEdge) {
			for (int i = 0; i<a.length; i++) a[i] = false;
		}
		for (Route r : this) {
			for (int i = 0; i<r.size(); i++) {
				final int a = r.get(i);
				final int b = r.get(i+1);
				positionForVertex[a] = i;
				routeForVertex[a] = r.index;
				containsEdge[a][b] = containsEdge[b][a] = true;
			}
		}
	}
	
	public void updatePositions(Route r) {
		for (int i = 0; i<r.size(); i++) {
			final int vertex = r.get(i);
			positionForVertex[vertex] = i;
			routeForVertex[vertex] = r.index;
		}
	}
	
	public boolean verify() {
		boolean ok = true;
		
		for (int i = 1; i<positionForVertex.length; i++) {
			if (get(routeForVertex[i]).get(positionForVertex[i]) != i) {
				System.err.println("Vertex " + i +" is not in the right place, check it's missing?");
				ok = false;
			}
		}
		
		for (Route r : this) {
			for (int i = 1; i< r.size(); i++) {
				if (routeForVertex[r.get(i)] != r.index) {
					ok = false;
					System.err.println("Route for vertex " + r.get(i) + " is invalid");
				}
				if (positionForVertex[r.get(i)] !=i) {
					ok = false;
					System.err.println("Position for vertex " + r.get(i) + " is invalid");
				}
				if (!containsEdge[r.get(i)][r.get(i+1)]) {
					System.err.println("Edge " + r.get(i) + "->" + r.get(i+1) + " is missing");
					ok = false;
				}
				final int v = r.get(i);
				final int p = r.get(i-1);
				final int s = r.get(i+1);
				for (int L = 0; L<containsEdge[v].length; L++) {
					if (containsEdge[v][L]) {
						if (L != p && L != s) {
							System.err.println("Superfluous edge: (" + v + ", " + L +"). Actual segment: .. " + p + ", " + v + ", " + s);
							ok = false;
						}
					}
				}
			}
		}
		return ok;
	}
	
	public int routeContaining(int v) {
		if (v == 0) return -1;
		return routeForVertex[v];
	}

	public int routePosition(int v) {
		if (v == 0) return 0;
		return positionForVertex[v];
	}
	
	private void addEdge(int here, int there) {
		if (containsEdge[here][there]) return;
		containsEdge[here][there] = containsEdge[there][here] = true;
		for (IEdgeListener listener : edgeListeners) {
			listener.edgeAdded(this, here, there);
		}
	}

	private void deleteEdge(int preThere, int there) {
		if (!containsEdge[preThere][there]) return;
		containsEdge[there][preThere] = containsEdge[preThere][there] = false;
		for (IEdgeListener listener : edgeListeners) {
			listener.edgeDeleted(this, preThere, there);
		}
	}

	public interface IEdgeListener {
		public void edgeAdded(Solution solution, int from, int to);
		public void edgeDeleted(Solution solution, int from, int to);
	}
	
	Set<IEdgeListener> edgeListeners = new HashSet<IEdgeListener>();
	
	public void addEdgeListener(IEdgeListener listener) {
		edgeListeners.add(listener);
	}
	
	public void removeEdgeListener(IEdgeListener listener) {
		edgeListeners.remove(listener);
	}

	/**
	 * Get the index of an empty route, if there is one
	 * @return the index of the first empty route
	 */
	public int getEmptyRoute() {
		for (Route r : this) {
			if (r.size() == 1) return r.index;
		}
		return -1;
	}

	/**
	 * Get the number of vertices in this solution (including depots)
	 * @return vertex count for solution;
	 */
	public int getVertexCount() {
		return positionForVertex.length;
	}

	/**
	 * Get the number of routes containing more than just the depot
	 * @return how many routes have customers in them
	 */
	public int activeRouteCount() {
		int count = 0;
		for (Route r : this) if (r.size() > 1) count++;
		return count;
	}

	public int vertexRelativeTo(int vertex, int offset) {
		if (vertex <= 0) {
			return get(-vertex).get(1);
		} else {
			return get(routeContaining(vertex)).get(routePosition(vertex)+offset);
		}
	}

	public void updateForChanges(IRouteModification[] changes) {
		IEdgeIterator ei;
		
		for (IRouteModification x : changes) {
			ei = x.deletedEdges();
			while (ei.hasNext()) {
				ei.next();
				deleteEdge(ei.from(), ei.to());
//				System.err.println(x.unmodifiedRoute().index + " - (" + ei.from() + ", " + ei.to() + ")");
			}
		}
		for (IRouteModification x : changes) {
			ei = x.createdEdges();
			while (ei.hasNext()) {
				ei.next();
				addEdge(ei.from(), ei.to());
//				System.err.println(x.unmodifiedRoute().index + " + (" + ei.from() + ", " + ei.to() + ")");
			}
		}
	}
	
	public void applyMove(IMove move) {
		IRouteModification [] mods = move.getChanges();
		updateForChanges(mods);
		move.apply();
		for (IRouteModification mod : mods)
			updatePositions(mod.unmodifiedRoute());
		if (Defines.VALIDATE) {
			if (!verify()) {
				if (move instanceof RecombiningMove) {
					System.err.println(((RecombiningMove)move).toLongString());
				} else {
					System.err.println(move);
				}
				System.err.println("Fail!");
			}
		}		
	}

	final public boolean containsEdge(int from, int to) {
		return containsEdge[from][to];
	}
}
