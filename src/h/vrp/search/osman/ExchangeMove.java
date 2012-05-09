package h.vrp.search.osman;

import h.vrp.model.IRouteModification;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.search.IMove;

public class ExchangeMove implements IMove {
	private Solution solution;
	private int v1;
	private int v2;

	public ExchangeMove(Solution solution, int v1, int v2) {
		this.solution = solution;
		this.v1 = v1;
		this.v2 = v2;
		
		int r1 = solution.routeContaining(v1);
		int r2 = (v2 <= 0) ? -v2 : solution.routeContaining(v2);
//		if (r1 == r2) {
//			System.err.println("WTF");
//		}
	}

	@Override
	public void apply() {
		if (v1 == v2) return;
		final Route r1 = solution.get(solution.routeContaining(v1));
		final Route r2 = solution.get(solution.routeContaining(v2));
		final int p1 = solution.routePosition(v1);
		final int p2 = solution.routePosition(v2);
		r1.set(p1, v2);
		r2.set(p2, v1);
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public String toString() {
		return "ExchangeMove [v1=" + v1 + ", v2=" + v2 + "]";
	}
	
	public IRouteModification[] getChanges() {
		IRouteModification[] rv = new IRouteModification[2];
		rv[0] = new ExchangeModification(this, false);
		rv[1] = new ExchangeModification(this, true);
		return rv;
	}
	
	class ExchangeModification implements IRouteModification {
		private boolean route1;
		private int pre, suc, ins, del;
		private Route route;
		
		public ExchangeModification(ExchangeMove move, boolean route1) {
			this.route1 = route1;
			this.route = solution.get(
					route1 ? (v1 <= 0 ? -v1 : solution.routeContaining(v1)) : (v2 <= 0 ? -v2 : solution.routeContaining(v2))
					);
			if (route1) {
				del = v1;
				ins = v2;
			} else {
				del = v2;
				ins = v1;
			}
			pre = move.solution.vertexRelativeTo(del, -1);
			suc = move.solution.vertexRelativeTo(del, +1);
			if (del <= 0) del = 0;
			if (ins <= 0) ins = 0;
		}
		@Override
		public IEdgeIterator addedEdges() {
			return PushMove.dei(pre, ins, suc);
		}

		@Override
		public IVertexIterator addedVertices() {
			return PushMove.svi(ins);
		}

		@Override
		public IEdgeIterator removedEdges() {
			return PushMove.dei(pre, del, suc);
		}

		@Override
		public IVertexIterator removedVertices() {
			return PushMove.svi(del);
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
			return removedEdges();
		}
		
	}
}
