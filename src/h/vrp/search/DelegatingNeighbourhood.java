package h.vrp.search;

import h.vrp.model.Solution;

/**
 * A neighbourhood which samples its children until one of them returns a non-null move.
 * @author hinton
 */
public class DelegatingNeighbourhood extends MultiNeighbourhood {
	public DelegatingNeighbourhood() {
		
	}
	@Override
	public IMove sample(Solution solution) {
		for (INeighbourhood n : subNeighbourhoods) {
			IMove m = n.sample(solution);
			if (m != null) return m;
		}
		return null;
	}
	
	public String toString() {
		return subNeighbourhoods.toString();
	}
	@Override
	public int size(Solution solution) {
		for (INeighbourhood n : subNeighbourhoods) {
			final int ns = n.size(solution);
			if (ns > 0) return ns; 
		}
		return 0;
	}
}
