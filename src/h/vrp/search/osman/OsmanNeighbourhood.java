package h.vrp.search.osman;

import h.util.random.HintonRandom;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;

public class OsmanNeighbourhood implements INeighbourhood {
	private HintonRandom random;

	public OsmanNeighbourhood(HintonRandom random) {
		this.random = random;
	}
	@Override
	public IMove sample(Solution solution) {
		//swap or push move; there are equally many of each, so 50/50 split
		final int activeRoutes = solution.activeRouteCount();
		
		//first pick a random vertex
		final int v1 = random.nextCustomer(solution);
		final int rv1 = solution.routeContaining(v1);
		
		//now choose what kind of move; exchange or push?
		if (activeRoutes > 1 && random.nextBoolean()) {
			//now find a customer on another route
			int[] routeSizes = new int[solution.size()];
			int i = 0;
			int total = 0;
			for (Route r : solution) {
				if (r.index != rv1)
					total += (routeSizes[i++] = r.size() - 1);
				else
					routeSizes[i++] = 0;
			}
			
			final Route r2 = solution.get(random.pickWeightedIndex(routeSizes, total));

			return new ExchangeMove(solution, v1, r2.at(1 + random.nextInt(r2.size()-1)));
		} else {
			//push move; put a vertex into another route somewhere
			int[] routeSizes = new int[solution.size()];
			int total = 0;
			for (Route r : solution) {
				if (r.index != rv1)
					total += (routeSizes[r.index] = r.size()); 
			}
			
			if (total == 0) {
				return new PushMove(solution, v1, -solution.getEmptyRoute());
			} else {
				final Route r2 = solution.get(random.pickWeightedIndex(routeSizes, total));
				final int pos = random.nextInt(r2.size());
				return new PushMove(solution, v1, pos == 0 ? -r2.index : r2.at(pos));
			}
		}
	}

	@Override
	public int size(Solution solution) {
		final int vc = solution.getVertexCount();
		return 2 * vc * vc;
	}
}
