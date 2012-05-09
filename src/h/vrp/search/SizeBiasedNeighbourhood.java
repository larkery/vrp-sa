package h.vrp.search;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;

public class SizeBiasedNeighbourhood extends MultiNeighbourhood {
	HintonRandom random;
	int[] sizes;
	
	public SizeBiasedNeighbourhood(HintonRandom random) {
		super();
		this.random = random;
	}
	

	@Override
	public void addNeighbourhood(INeighbourhood n) {
		super.addNeighbourhood(n);
		sizes = new int[subNeighbourhoods.size()];
	}


	@Override
	public IMove sample(Solution solution) {
		int total = 0;
		for (int i = 0; i<sizes.length; i++) {
			sizes[i] = subNeighbourhoods.get(i).size(solution);
			total+=sizes[i];
		}
		return subNeighbourhoods.get(random.pickWeightedIndex(sizes, total)).sample(solution);
	}
}
