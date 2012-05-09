package h.vrp.search.cl;

import h.vrp.model.Solution;

public final class Crossing {
	public int e11, e12, e21, e22;

	public Crossing(int i, int j, int i2, int j2) {
		int t;
		if (i > j) {
			t = i;
			i = j;
			j = t;
		}
		if (i2 > j2) {
			t = i2;
			i2 = j2;
			j2 = t;
		}
		if (i < i2 || (i == i2 && j < j2)) {
			e11 = i;
			e12 = j;
			e21 = i2;
			e22 = j2;
		} else {
			e11 = i2;
			e12 = j2;
			e21 = i;
			e22 = j;
		}
	}

	final public boolean containsEdge(int from, int to) {
		int t;
		if (from > to) {
			t = from;
			from = to;
			to = t;
		}
		return (e11 == from && e12 == to) ||
				(e21 == from && e22 == to);
	}

	@Override
	final public String toString() {
		return "(" + e11 + ", " + e12 + " x " + e21
				+ ", " + e22 + ")";
	}

	@Override
	final public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + e11;
		result = prime * result + e12;
		result = prime * result + e21;
		result = prime * result + e22;
		return result;
	}

	@Override
	final public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Crossing other = (Crossing) obj;
		if (e11 != other.e11)
			return false;
		if (e12 != other.e12)
			return false;
		if (e21 != other.e21)
			return false;
		if (e22 != other.e22)
			return false;
		return true;
	}

	/**
	 * Whether c shares an edge with this crossing; possible results are:
	 * 
	 * <ol>
	 * <li> 0 - no edge is common </li>
	 * <li> 1 - Edge 1 of this crossing equals edge 1 of the other crossing </li>
	 * <li> 2 - Edge 2 of this crossing equals edge 1 of the other crossing </li>
	 * <li> 3 - Edge 1 of this crossing equals edge 2 of the other crossing </li>
	 * <li> 4 - Edge 2 of this crossing equals edge 2 of the other crossing </li>
	 * </ol>
	 * @param c
	 * @return
	 */
	final public int sharesEdgeWith(Crossing c) {
		if (c.e11 == e11 && c.e12 == e12) return 1;
		if (c.e11 == e21 && c.e12 == e22) return 2;
		if (c.e21 == e11 && c.e22 == e12) return 3;
		if (c.e21 == e21 && c.e22 == e22) return 4;
//		if (c.e11 == e11 && c.e12 == e22) return 1;
//		if (c.e22 == e22 && c.e21 == e21) return 2;
//		if (c.e11 == e21 && c.e12 == e22) return 3;
//		if (c.e21 == e11 && c.e22 == e12) return 4;
			
		return 0;
	}

	final public int startE2(Solution solution) {
		int a = Math.min(solution.routePosition(e21), solution.routePosition(e22));
		int b = Math.max(solution.routePosition(e21), solution.routePosition(e22));
		
		//NEXT integrity check for reverse lookup tables.
		
		if (a == 0 && b > 1) return b;
		return a;
	}

	final public int startE1(Solution solution) {
		int a = Math.min(solution.routePosition(e11), solution.routePosition(e12));
		int b = Math.max(solution.routePosition(e11), solution.routePosition(e12));
		
		if (a == 0 && b > 1) return b;
		return a;
	}

	final public int routeContainingE1(Solution solution) {
		return solution.routeContaining(e11 == 0 ? e12 : e11);
	}

	final public int routeContainingE2(Solution solution) {
		return solution.routeContaining(e21 == 0 ? e22 : e21);
	}
	
	
	public final int firstIndexInRoute(Solution solution, int r) {
		int rv = solution.size();
		if (routeContainingE1(solution) == r) {
			rv = startE1(solution); 
		}
		if (routeContainingE2(solution) == r) {
			rv = Math.min(rv, startE2(solution));
		}
		return rv;
	}

	public final int otherRoute(Solution solution, int r) {
		final int k = routeContainingE1(solution); 
		
		return (k == r) ? routeContainingE2(solution) : k;
	}

	final public int oneRoute(Solution solution) {
		return routeContainingE1(solution);
	}

	final public int codeE1(Solution solution) {
		return code(e11, e12, solution);
	}
	
	final int code(int a, int b, Solution solution) {
		if (a == 0) {
			return -solution.routeContaining(b);
		} else {
			if (solution.routePosition(a) < solution.routePosition(b)) {
				return a;
			} else {
				return b;
			}
		}
	}
	
	final public int codeE2(Solution solution) {
		return code(e21, e22, solution);
	}
}
