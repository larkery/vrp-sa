package h.vrp.solcons;

import h.math.SolutionDistribution;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RandomConstructor implements ISolutionConstructor {
	private HintonRandom random;
	private boolean fullyRandom;
	private boolean ensureFeasibility;
	private int maxTries;
	/**
	 * Get a new random constructor
	 * @param random the source of randomness
	 * @param fullyRandom whether to allow any number of routes, or the instance-specified number
	 * @param ensureFeasibility whether the returned solution MUST be feasible
	 */
	public RandomConstructor(HintonRandom random, boolean fullyRandom, boolean ensureFeasibility, int maxTries) {
		this.random = random;
		this.fullyRandom = fullyRandom;
		this.ensureFeasibility = ensureFeasibility;
		this.maxTries = maxTries;
		if (ensureFeasibility && !fullyRandom) {
			System.err.println("Warning: random constructor will basically never produce feasible solutions if not fully random");
		}
	}
	
	public RandomConstructor(HintonRandom random, boolean fullyRandom, boolean ensureFeasibility) {
		this(random, fullyRandom, ensureFeasibility, 10000);
	}
	
	@Override
	public Solution createSolution(Instance instance) {
		int counter = maxTries;
		while (counter > 0) {
		Solution sol = reallyCreateSolution(instance);
			if (ensureFeasibility) {
				DeltaEvaluator e = new DeltaEvaluator(instance, sol);
				if (e.isFeasible())
					return sol;
			} else {
				return sol;
			}
			counter--;
		}
		System.err.println("Warning: random constructor did not find a feasible solution. You're getting an infeasible one.");
		return null;
	}
	
	protected Solution reallyCreateSolution(Instance instance) {
		if (fullyRandom) {
			SolutionDistribution sd = SolutionDistribution.getInstance(instance.getPoints().size()-1);
			return createSolution(instance, sd.pick(random));
		}
		return createSolution(instance, instance.getVehicleCount());
	}

	protected Solution createSolution(Instance instance, int routeCount) {
		int dcCount = instance.getPoints().size() - 1;
		
		Solution sol = new Solution((int)((1+routeCount)*1.5), instance.getPoints().size()); //hack
		//I have to do n - 1 choose r - 1
		List<Integer> breakpoints = new ArrayList<Integer>(dcCount - 1);
		for (int i = 1; i<dcCount; i++) {
			breakpoints.add(i);
		}
		Collections.shuffle(breakpoints, random);
		breakpoints.add(0, 0);
		breakpoints.subList(routeCount, breakpoints.size()).clear();
		Collections.sort(breakpoints);
		breakpoints.add(dcCount);
		
		List<Integer> pi = new ArrayList<Integer>(dcCount);
		for (int i = 1; i<=dcCount; i++) {
			pi.add(i);
		}
		Collections.shuffle(pi, random);

		Iterator<Integer> breakIterator = breakpoints.iterator();
	
		int cbreak = breakIterator.next();
		
		Route r = null;
		int ri = -1;
		for (int i = 0; i<pi.size(); i++) {
			if (i == cbreak) {
				ri++;
				r = sol.get(ri);
				r.add(0);
				cbreak = breakIterator.next();
			}
			r.add(pi.get(i));
		}
		
		for (; ri < sol.size(); ri++) {
			if (sol.get(ri).isEmpty()) sol.get(ri).add(0);
		}
		
		sol.initialize();
		
		return sol;
	}
}
