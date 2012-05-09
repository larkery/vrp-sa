package h.vrp.search.blind;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.cl.noptk.RecombiningMove;

/**
 * An entirely random neighbourhood. Currently does the same thing as an edge weighted one
 * but makes all edges equally weighted.
 * @author hinton
 */
public class BlindNeighbourhood implements INeighbourhood {
	private HintonRandom random;
	private int min;
	private int max;

	public BlindNeighbourhood(HintonRandom random, int min, int max) {
		this.min = min;
		this.max = max;
		this.random = random;
	}
	
	public BlindNeighbourhood(HintonRandom random) {
		this(random, 2, 4);
	}

	@Override
	public IMove sample(Solution solution) {
		//pick a random edge
		final int edgeCount = random.nextInt(max-min+1) + min;
		
		int [] c = new int[edgeCount];
		for (int i = 0; i<edgeCount;) {
			c[i] = random.nextCodedVertex(solution);
			boolean done = true;
			for (int j = 0; i<i;j++) {
				done = done && c[j] != c[i];
			}
			if (done) i++;
		}
		
		return new RecombiningMove(solution, c, random);
	}

	@Override
	public int size(Solution solution) {
		return solution.size() * (solution.size() - 1);
	}
}
