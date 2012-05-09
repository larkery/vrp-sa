package h.vrp.search.osman;

import h.vrp.model.IRouteModification;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.IRouteModification.IEdgeIterator;
import h.vrp.model.IRouteModification.IVertexIterator;
import h.vrp.search.IMove;

public class PushMove implements IMove {

	private Solution solution;
	private int v1;
	private int v2;

	public PushMove(Solution solution, int v1, int v2) {
		this.solution = solution;
		this.v1 = v1;
		this.v2 = v2;
		int r1 = solution.routeContaining(v1);
		int r2 = (v2 <= 0) ? -v2 : solution.routeContaining(v2);
	}

	@Override
	public void apply() {
		if (v1 == v2) return;
		final Route r1 = solution.get(solution.routeContaining(v1));
		final int p1 = solution.routePosition(v1);
		final Route r2 = 
			v2 <= 0 ? solution.get(-v2) :
				solution.get(solution.routeContaining(v2));
		final int p2 = v2 <= 0 ? 0 : solution.routePosition(v2);
			
		r1.remove(p1);
		r2.add(p2+1, v1);
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public String toString() {
		return "PushMove [v1=" + v1 + ", v2=" + v2 + "]";
	}

	public IRouteModification[] getChanges() {
		IRouteModification[] rv = new IRouteModification[2];
		rv[0] = new PushModification(this, false);
		rv[1] = new PushModification(this, true);
		return rv;
	}

	class PushModification implements IRouteModification {
		private boolean route1;
		int pre, suc, ins;
		private Route route;
		public PushModification(PushMove move, boolean route1) {
			this.route1 = route1;
			if (route1) {
				this.route = solution.get(v1 <= 0 ? -v1 : solution.routeContaining(v1)); 
				pre = solution.vertexRelativeTo(v1, -1);
				suc = solution.vertexRelativeTo(v1, 1);
			} else {
				this.route = solution.get(v2 <= 0 ? -v2 : solution.routeContaining(v2));
				pre = v2 <= 0 ? 0 : v2;
				suc = solution.vertexRelativeTo(v2, 1);
			}
			ins = v1 <= 0 ? 0 : v1;
		}
		@Override
		public IEdgeIterator addedEdges() {
			if (route1) {
				return sei(pre, suc);
			} else {
				return dei(pre, ins, suc);
			}
		}

		@Override
		public IVertexIterator addedVertices() {
			if (route1) {
				return IRouteModification.emptyVertexIterator;
			} else {
				return svi(ins);
			}
		}

		@Override
		public IEdgeIterator removedEdges() {
			if (route1) {
				return dei(pre, ins, suc);
			} else {
				return sei(pre, suc);
			}
		}

	
		@Override
		public IVertexIterator removedVertices() {
			if (route1) {
				return svi(ins);
			} else {
				return IRouteModification.emptyVertexIterator;
			}
		}

		@Override
		public Route unmodifiedRoute() {
			return route;
		}
		@Override
		public IEdgeIterator createdEdges() {
			return addedEdges();
		}
		@Override
		public IEdgeIterator deletedEdges() {
			if (!route1 && route.size() == 2) return emptyEdgeIterator;
			else return removedEdges();
		}
		
		
		public String toString() {
			StringBuilder sb = new StringBuilder() ;
			
			sb.append("PushModification\n");
			sb.append("Added Edges: " + str(addedEdges()) + "\n");
			sb.append("Removed Edges: " + str(removedEdges()) + "\n");
			sb.append("Added Vertices: " + str(addedVertices()) + "\n");
			sb.append("Removed Vertices: " + str(removedVertices()) + "\n");
			
			return sb.toString();
		}
	}
	
	protected static IEdgeIterator sei(final int a, final int b) {
		return new IEdgeIterator() {
			boolean hasNext = true;
			@Override
			public int to() {
				return b;
			}
			
			@Override
			public void next() {
				hasNext = false;
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}
			
			@Override
			public int from() {
				return a;
			}
		};
	}
	
	protected static IEdgeIterator dei(final int a, final int b, final int c) {
		return new IEdgeIterator() {
			char pos = 0;
			int from, to;
			@Override public int from() {return from;}
			@Override public int to() {return to;}
			
			@Override
			public void next() {
				switch(pos) {
				case 0:
					from = a;
					to = b;
					break;
				case 1:
					from = b;
					to = c;
					break;
				}
				pos++;
			}
			
			@Override
			public boolean hasNext() {
				return pos < 2;
			}
		};
		
		
	}
	
	protected static IVertexIterator svi(final int del2) {
		return new IVertexIterator() {
			boolean hasNext = true;
			@Override
			public int next() {
				hasNext = false;
				return del2;
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}
		};
	}
	
	static String str(IEdgeIterator ei) {
		StringBuilder sb = new StringBuilder();
		while (ei.hasNext()) {
			ei.next();
			sb.append(" (" + ei.from() + ", " + ei.to() + ")");
		}
		return sb.toString();
	}static String str(IVertexIterator vi) {
		StringBuilder sb = new StringBuilder();
		while (vi.hasNext()) {
			sb.append(" " + vi.next());
		}
		return sb.toString();
	}
}
