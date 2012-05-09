package h.vrp.search;

import h.vrp.model.Instance;
import h.vrp.model.Solution;

public interface INeighbourhoodConstructor {
	public INeighbourhood createNeighbourhood(Instance i, Solution s);
}
