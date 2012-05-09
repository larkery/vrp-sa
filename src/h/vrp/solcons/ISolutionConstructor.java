package h.vrp.solcons;

import h.vrp.model.Instance;
import h.vrp.model.Solution;

public interface ISolutionConstructor {
	public Solution createSolution(Instance instance) ;
}
