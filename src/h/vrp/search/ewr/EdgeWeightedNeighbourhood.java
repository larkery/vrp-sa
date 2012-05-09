package h.vrp.search.ewr;

import h.defines.Defines;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.Solution.IEdgeListener;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.cl.noptk.RecombiningMove;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A neighbourhood which generates n-opt-k moves in which each edge is selected with frequency proportional
 * to its cost; the idea is that we spend more time deleting heavy edges?
 * @author hinton
 *
 */
public class EdgeWeightedNeighbourhood implements INeighbourhood, IEdgeListener {
	Set<WeightedEdge> edges = new HashSet<WeightedEdge>();
	private Instance instance;
	float totalWeight;
	protected HintonRandom random;
	
	public class WeightedEdge implements Comparable<WeightedEdge> {
		int from, to;
		float weight;
		public float getWeight() {
			return weight;
		}
		public WeightedEdge(int from, int to, float weight) {
			super();
			replace(from, to, weight);
		}
		
		public void replace(int from, int to, float weight) {
			this.from = Math.min(from, to);
			this.to = Math.max(from, to);
			this.weight = weight;
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
			WeightedEdge other = (WeightedEdge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (from != other.from)
				return false;
			if (to != other.to)
				return false;
			return ((other.from == from) && (other.to == to) ||
					(other.to == from) && (other.from == to));
		}
		private EdgeWeightedNeighbourhood getOuterType() {
			return EdgeWeightedNeighbourhood.this;
		}
		public int getFrom() {
			return from;
		}
		public int getTo() {
			return to;
		}
		@Override
		public int compareTo(WeightedEdge o) {
			if (o.weight < weight) {
				return -1;
			} else if (o.weight == weight) {
				return 0;
			} else {
				return 1;
			}
		}
		
		public int getRoute(Solution solution) {
			if (from == 0) {
				if (to == 0) {
					return solution.getEmptyRoute();
				} else {
					return solution.routeContaining(to);
				}
			} else {
				return solution.routeContaining(from);
			}
		}
		public String toString() {
			return "(" + from + "->" + to + "  " + weight + ")";
		}
		
		public int codeFrom(Solution solution) {
			if (from == 0) {
				if (solution.routePosition(to) == 1) {
					return -getRoute(solution);
				} else {
					if (to == 0) {
						return -getRoute(solution);
					} else {
						return to;
					}
				}
			} else {
				if (solution.routePosition(from) < solution.routePosition(to)) return from;
				else return to;
			}
		}
	}
	
	public EdgeWeightedNeighbourhood(Instance instance, Solution initial, HintonRandom random) {
		initial.addEdgeListener(this);
		this.instance = instance;
		this.random = random;
		totalWeight = 0;
		//force add edges
		for (Route r : initial) {
			for (int i = 0; i<r.size(); i++) {
				edgeAdded(initial, r.at(i), r.at(i+1));
			}
		}
	}
	
	@Override
	public IMove sample(Solution solution) {
		
		if (Defines.VALIDATE) validate(solution);
		
		WeightedEdge e1 = pickAnEdge();
		WeightedEdge e2 = pickAnotherEdge(e1);
		//now, if e1 and e2 are in the same route, we should do an n-opt-1 move, otherwise
		//we should do an n-opt-2 move.
		final int r1 = e1.getRoute(solution);
		final int r2 = e2.getRoute(solution);
		
		if (r1 == r2) {
			//either we do a 2opt1 or a 3opt1
			if (random.nextBoolean()) {
				//2opt1
				return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution)}, random);
//				int a = solution.routePosition(e1.from);
//				int b = solution.routePosition(e2.from);
//				return new M2O1(solution.get(r1), Math.min(a, b), Math.max(a, b));
			} else {
				WeightedEdge e3 = pickAnotherEdge(e1, e2);
				final int r3 = e3.getRoute(solution);
				return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), 
						e2.codeFrom(solution), e3.codeFrom(solution)}, random);
//				if (r3 == r2) {
//					//3opt1
//					
//					
////					int a = solution.routePosition(e1.from);
////					int b = solution.routePosition(e2.from);
////					int c = solution.routePosition(e3.from);
////					//sort edges
////					if (b < a) { int t = b; b = a; a = t; }
////					if (c < b) { int t = c; c = b; b = t; }
////					if (b < a) { int t = b; b = a; a = t; }
////					
////					if (random.nextBoolean()) {
////						//cut segment a..b
////						return new M3O1(solution.get(r1), a, b, c);
////					} else {
////						//cut segment b..c
////						return new M3O1(solution.get(r1), b, c, a);
////					}
//				} else {
//					//3opt2, cut e1..e2 and paste into e3
//					int a = solution.routePosition(e1.from);
//					int b = solution.routePosition(e2.from);
//					return new M3O2(solution.get(r1), solution.get(r3), Math.min(a, b), Math.max(a, b), 
//							solution.routePosition(e3.from));
//				}
			}
		} else {
			switch(random.nextInt(3)) {
			case 0:
				
				return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution)}, random);
//				return new M2O2(solution.get(r1), solution.get(r2), solution.routePosition(e1.from), solution.routePosition(e2.from));
			case 2:
				
				//4opt2 means picking two more edges on the route
				if ((e1.from == 0 && e1.to == 0) || (e2.from == 0 && e2.to == 0))
					return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution)}, random);
				WeightedEdge e3 = pickAnotherEdgeOnRoute(e1, solution);
				WeightedEdge e4 = pickAnotherEdgeOnRoute(e2, solution);
//				final int A = Math.min(solution.routePosition(e1.from), solution.routePosition(e3.from));
//				final int B = Math.max(solution.routePosition(e1.from), solution.routePosition(e3.from));
//				final int C = Math.min(solution.routePosition(e2.from), solution.routePosition(e4.from));
//				final int D = Math.max(solution.routePosition(e2.from), solution.routePosition(e4.from));
				return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution),
						e3.codeFrom(solution), e4.codeFrom(solution)}, random);
//				return new M4O2(solution.get(r1), solution.get(r2), A, B, C, D);
			case 1:
				
				if (e1.from == 0 && e1.to == 0)
					return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution)}, random);
				WeightedEdge e5 = pickAnotherEdgeOnRoute(e1, solution);
				return new RecombiningMove(solution, new int[] {e1.codeFrom(solution), e2.codeFrom(solution), e5.codeFrom(solution)}, random);
//				return new M3O2(solution.get(r1), solution.get(r2), 
//						Math.min(solution.routePosition(e5.from), solution.routePosition(e1.from)),
//						Math.max(solution.routePosition(e5.from), solution.routePosition(e1.from)),
//						solution.routePosition(e2.from));
			}
		}
		
		return null;
	}
	
	private void validate(Solution solution) {
		boolean valid = true;
		for (WeightedEdge e : edges) {
			if (!solution.containsEdge(e.from, e.to)) {
				System.err.println("Bogus edge " + e); valid = false;
			}
		}
		WeightedEdge find = new WeightedEdge(0, 0, 0);
		for (Route r : solution) {
			for (int i = 0; i<r.size(); i++) {
				find.replace(r.at(i), r.at(i+1), 0);
				if (!edges.contains(find)) {
					System.err.println("Missing edge: " + find); valid = false;
				}
			}
		}
		if (!valid) throw new ArrayIndexOutOfBoundsException();
		
	}

	protected WeightedEdge pickAnotherEdgeOnRoute(WeightedEdge e1, Solution solution) {
		final int ix = e1.getRoute(solution);
//		if (ix < 0) {
//			System.err.println("WTF");
//		}
		final Route r = solution.get(e1.getRoute(solution));
		double pin = random.nextDouble() * (r.getCost() - e1.weight);
		float acc = 0;
		WeightedEdge temp = new WeightedEdge(0,0,0);
		for (int i = 0; i<r.size(); i++) {
			temp.replace(r.at(i), r.at(i+1), getWeight(r.at(i), r.at(i+1)));
			if (temp.equals(e1)) continue;
			
			if (acc <= pin && (acc + temp.weight) >= pin) {
				break;
			}
			acc += temp.weight;
		}
		return temp;
	}
	
	protected WeightedEdge pickAnotherEdge(WeightedEdge e1, WeightedEdge e2) {
		double pin = random.nextDouble() * (totalWeight - (e1.weight + e2.weight));
		double acc = 0;
		WeightedEdge we = null;
		Iterator<WeightedEdge> wei = edges.iterator();
		while (wei.hasNext()) {
			we = wei.next();
			if (we.equals(e1) || we.equals(e2)) continue;
			if (pin >= acc && pin <= acc + we.weight) {
				return we; 
			}
			acc += we.weight;
		}
		return we;
	}

	/**
	 * Randomly select a weighted edge
	 * @return
	 */
	protected WeightedEdge pickAnEdge() {
		return pickAnotherEdge(null);
	}

	/**
	 * Randomly select a weighted edge which isn't w.
	 * @param w
	 * @return
	 */
	protected WeightedEdge pickAnotherEdge(WeightedEdge w) {
		double pin = random.nextDouble() * (totalWeight - (w == null ? 0 : w.weight));
		double acc = 0;
		WeightedEdge we = null;
		Iterator<WeightedEdge> wei = edges.iterator();
		while (wei.hasNext()) {
			we = wei.next();
			if (we.equals(w)) continue;
			if (pin >= acc && pin <= acc + we.weight) {
				return we; 
			}
			acc += we.weight;
		}
		return we;
	}
	
	@Override
	public void edgeAdded(Solution solution, int from, int to) {
//		if (from == to && from != 0) {
//			throw new ArrayIndexOutOfBoundsException();
//		}
		WeightedEdge e = new WeightedEdge(from, to, getWeight(from, to));
//		System.err.println("+" + e);
		edges.add(e);
		totalWeight += e.weight; 
	}

	@Override
	public void edgeDeleted(Solution solution, int from, int to) {
		WeightedEdge e = new WeightedEdge(from, to, getWeight(from, to));
//		System.err.println("-" + e);
		edges.remove(e);
		totalWeight -= e.weight;
	}
	
	public String toString() {
		return totalWeight + " " + edges;
	}
	
	protected float getWeight(int from, int to) {
		if (from == to) {
			return 1; //allow moves involving empty routes
		} else {
			return instance.getEdgeCost(from, to);
		}
	}

	@Override
	public int size(Solution solution) {
		return edges.size() * edges.size();
	}
}
