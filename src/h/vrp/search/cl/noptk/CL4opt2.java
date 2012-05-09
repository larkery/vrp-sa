package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.cl.Crossing;
import h.vrp.search.cl.CrossingTracker;

import java.util.ArrayList;
import java.util.List;

public class CL4opt2 extends CLNoptK {

	public CL4opt2(CrossingTracker crossingList, HintonRandom random) {
		super(crossingList, random);
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("unchecked")
	@Override
	public IMove sample(Solution solution) {
		List<Crossing>[][] crossings = new List[solution.size()][solution.size()];
		List<List<Crossing>> lists = new ArrayList<List<Crossing>>();
		
		for (Crossing c : crossingList) {
			final int r1 = c.routeContainingE1(solution);
			final int r2 = c.routeContainingE2(solution);
			
			if (r1 != r2) {
				if (crossings[r1][r2] == null) {
					crossings[r1][r2] =
						crossings[r2][r1] =
							new ArrayList<Crossing>();
				}
				final List<Crossing> thisPair = crossings[r1][r2];
				if (thisPair.size() == 1)
					lists.add(thisPair);
				thisPair.add(c);
			}
		}
		
		if (lists.size() > 0) {
			final List<Crossing> chosenPair = lists.get(random.nextInt(lists.size()));
			
			final int c1ix = random.nextInt(chosenPair.size());
			final int c2ix = random.nextIntExcluding(chosenPair.size(), c1ix);
			
			final Crossing c1 = chosenPair.get(c1ix);
			final Crossing c2 = chosenPair.get(c2ix);
			
			return new RecombiningMove(solution, new int[] {
					c1.codeE1(solution), c1.codeE2(solution),
					c2.codeE1(solution), c2.codeE2(solution)
			}, random);
			
//			//swap chunks; what about reversals?!?!
//			final int r1 = c1.routeContainingE1(solution);
//			final int r2 = c1.routeContainingE2(solution);
//			final int r1i1 = c1.startE1(solution);
//			final int r2i1 = c1.startE2(solution);
//			int r1i2;
//			int r2i2;
//			if (c2.routeContainingE1(solution) == r1) {
//				r1i2 = c2.startE1(solution);
//				r2i2 = c2.startE2(solution);
//			} else {
//				r1i2 = c2.startE2(solution);
//				r2i2 = c2.startE1(solution);
//			}
//			return new M4O2(solution.get(r1), solution.get(r2),  Math.min(r1i1, r1i2), Math.max(r1i1, r1i2), Math.min(r2i1, r2i2), Math.max(r2i1, r2i2));
////			return new M4O2(solution, r1, r2, Math.min(r1i1, r1i2), Math.max(r1i1, r1i2), Math.min(r2i1, r2i2), Math.max(r2i1, r2i2));
		}
		
		return null; 
	}

	@Override
	public int size(Solution solution) {
		int s = crossingList.size() * crossingList.size();
		return s*s;
	}

}
