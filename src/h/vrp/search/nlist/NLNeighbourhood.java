package h.vrp.search.nlist;

import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.cl.noptk.RecombiningMove;

public class NLNeighbourhood implements INeighbourhood {
	private NeighbourhoodList neighbourhoodList;
	private HintonRandom random;
	private int size_;

	public NLNeighbourhood(Instance instance, int listSize, HintonRandom random) {
		this.random = random;
		this.neighbourhoodList = new NeighbourhoodList(instance, listSize);
		size_ = instance.getPoints().size();
		size_ *= size_;
	}
	
	@Override
	public IMove sample(Solution solution) {
		int kill = 2 + random.nextInt(2);
		int[] kills = new int[kill];
		int picked = 0;
		while (picked < kills.length) {
			final int v = random.nextCustomer(solution);
			kills[picked++] = v;
			if (picked < kills.length) {
				final int n = random.pick(neighbourhoodList.getNeighbours(v));
				if (n == 0) {
					kills[picked++] = -random.nextInt(solution.size());
				} else {
					kills[picked++] = n;
				}
			}
		}
		
		return new RecombiningMove(solution, kills, random);
	}

	@Override
	public int size(Solution solution) {
		return size_;
	}
}
