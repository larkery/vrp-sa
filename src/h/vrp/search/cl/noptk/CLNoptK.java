package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.search.INeighbourhood;
import h.vrp.search.cl.Crossing;
import h.vrp.search.cl.CrossingTracker;

import java.util.ArrayList;
import java.util.List;

public abstract class CLNoptK implements INeighbourhood {
	protected CrossingTracker crossingList;
	protected HintonRandom random;
	protected List<Crossing> temp = new ArrayList<Crossing>();

	public CLNoptK(CrossingTracker crossingList, HintonRandom random) {
		this.crossingList = crossingList;
		this.random = random;
	}
}
