package h.vrp.search.cl;

import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.cl.noptk.RecombiningMove;

public class CLNeighbourhood2 extends CLNeighbourhood {
	public CLNeighbourhood2(Instance instance, Solution solution,
			HintonRandom random) {
		super(instance, solution, random);
	}

	@Override
	public IMove sample(Solution solution) {
		if (crossings.size() == 0) return null;
		else if (crossings.size() == 1 || random.nextBoolean()) return sample1(solution);
		else return sample2(solution);
	}


	private IMove sample2(Solution solution2) {
		int ci = random.nextInt(crossings.size());
		int ci2 = random.nextInt(crossings.size()-1);
		if (ci2 == ci) ci2++;
		Crossing picked1 = null;
		Crossing picked2 = null;
		for (Crossing c : crossings) {
			if (ci == 0) {
				picked1 = c;
			}
			if (ci2 == 0) {
				picked2 = c;
			}
			ci--;
			ci2--;
			if (picked1 != null & picked2 != null)
				break;
		}

		int[] cuts = new int[] {
			picked1.codeE1(solution2), picked1.codeE2(solution2),
			picked2.codeE1(solution2), picked2.codeE2(solution2)
		};
		
		return new RecombiningMove(solution2, cuts, random);
	}

	private IMove sample1(Solution solution2) {
		int ci = random.nextInt(crossings.size());
		Crossing picked = null;
		for (Crossing c : crossings) {
			if (ci == 0) {
				picked = c;
				break;
			}
			ci--;
		}
		
		int[] cuts = new int[2];
		cuts[0] = picked.e11 == 0 ? -picked.routeContainingE1(solution2) : picked.e11;
		cuts[1] = picked.e21 == 0 ? -picked.routeContainingE2(solution2) : picked.e21;
		
		return new RecombiningMove(solution2, cuts, random);
	}
}
