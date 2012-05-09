package h.vrp.search.cl;

import h.defines.Defines;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.RandomMultiNeighbourhood;
import h.vrp.search.cl.noptk.CL2opt1;
import h.vrp.search.cl.noptk.CL2opt2;
import h.vrp.search.cl.noptk.CL3opt1;
import h.vrp.search.cl.noptk.CL3opt2;
import h.vrp.search.cl.noptk.CL4opt2;

/**
 * My second implementation of the crossing list neighbourhood. Uses a {@link CrossingTracker} to keep
 * a list of crossings in the given {@link Solution}, and then generates moves accordingly. 
 */
public class CLNeighbourhood implements INeighbourhood {
	private Solution solution;
	protected CrossingTracker crossings;
	RandomMultiNeighbourhood delegate;
	protected HintonRandom random;
	protected Instance instance;
	
	void validate(Solution solution) {
		CrossingTracker nct = new CrossingTracker(instance, solution);
		for (Crossing c : nct) {
			if (!crossings.crossings.contains(c)) {
				System.err.println("Crossing missing: " + c);
			}
		}
		for (Crossing c : crossings) {
			if (!nct.crossings.contains(c)) {
				System.err.println("Extra crossing: " + c);
			}
		}
	}
	
	public CLNeighbourhood(Instance instance, Solution solution, HintonRandom random) {
		this.solution = solution;
		this.random = random;
		this.instance = instance;
		crossings = new CrossingTracker(instance, solution);
		delegate = new RandomMultiNeighbourhood(random);
		
		delegate.addNeighbourhood(new CL2opt1(crossings, random), 2);
		delegate.addNeighbourhood(new CL2opt2(crossings, random));
		delegate.addNeighbourhood(new CL3opt1(crossings, random));
		delegate.addNeighbourhood(new CL3opt2(crossings, random));
		delegate.addNeighbourhood(new CL4opt2(crossings, random));
	}

	@Override
	public IMove sample(Solution solution) {
		if (Defines.VALIDATE) {
			validate(solution);
		}
		return delegate.sample(solution);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		/*
		 * To be a good citizen, disconnect the crossing tracker from the solution.
		 */
		solution.removeEdgeListener(crossings);
	}
	
	public String toString() {
		return crossings.toString();
	}

	public Iterable<Crossing> getCrossings() {
		return crossings;
	}

	@Override
	public int size(Solution solution) {
		return crossings.size();
	}
}
