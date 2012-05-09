package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.cl.Crossing;
import h.vrp.search.cl.CrossingTracker;

public class CL2opt1 extends CLNoptK {
	public CL2opt1(CrossingTracker crossingList, HintonRandom random) {
		super(crossingList, random);
	}

	@Override
	public IMove sample(Solution solution) {
		for (Crossing c : crossingList) {
			final int r1 = solution.routeContaining(c.e11 == 0 ? c.e12 : c.e11);
			final int r2 = solution.routeContaining(c.e21 == 0 ? c.e22 : c.e21);
			if (r1 == r2) {
				temp.add(c);
			}
		}
		
		if (temp.size() == 0) return null;
		
		
		
		Crossing c = temp.get(random.nextInt(temp.size()));
		
		temp.clear();
		return new RecombiningMove(solution, new int[] {c.codeE1(solution), c.codeE2(solution)}, random);
		
//		final int routeix = solution.routeContaining(c.e11 == 0 ? c.e12 : c.e11);
//		final int a = Math.min(solution.routePosition(c.e11), solution.routePosition(c.e12));
//		final int b = Math.min(solution.routePosition(c.e21), solution.routePosition(c.e22));
//
//		IMove result = new M2O1(solution.get(routeix), a, b);
//		
//		temp.clear();
//		return result;
	}

	@Override
	public int size(Solution solution) {
		return crossingList.size();
	}
}
