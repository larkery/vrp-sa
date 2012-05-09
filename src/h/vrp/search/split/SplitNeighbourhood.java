package h.vrp.search.split;

import h.util.random.HintonRandom;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;

/**
 * A neighbourhood which generates moves that split routes into parts
 * because CL and so on cannot do that.
 * @author hinton
 *
 */
public class SplitNeighbourhood implements INeighbourhood {
	private HintonRandom random;
	public SplitNeighbourhood(HintonRandom random) {
		this.random = random;
	}
	@Override
	public IMove sample(Solution solution) {
		final int ri = solution.getEmptyRoute();
		if (ri == -1) 
			return null;
		
		final int vertex = random.nextInt(solution.getVertexCount() - 1) + 1;
		final Route route = solution.get(solution.routeContaining(vertex));
		final int position = solution.routePosition(vertex);
		
		return null;//new M2O2(route, solution.get(ri), position, 0);
	}
	
	@Override
	public int size(Solution solution) {
		if (solution.getEmptyRoute() == -1) return 0;
		else return solution.getVertexCount();
	}
	
}
