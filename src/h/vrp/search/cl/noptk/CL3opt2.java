package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.cl.CrossingTracker;

public class CL3opt2 extends CL3opt1 {

	public CL3opt2(CrossingTracker crossingList, HintonRandom random) {
		super(crossingList, random);
	}

	@Override
	public IMove sample(Solution solution) {
		return super.get3opt(solution, false);
	}

}
