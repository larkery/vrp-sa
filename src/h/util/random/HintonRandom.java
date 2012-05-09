package h.util.random;
import h.vrp.model.Solution;

import java.util.List;
import java.util.Random;

public class HintonRandom extends Random {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4563479506680992884L;

	public HintonRandom() {
		super();
	}

	public HintonRandom(long seed) {
		super(seed);
	}

	public int nextIntExcluding(int range, int skip) {
		int i = super.nextInt(range-1);
		if (i >= skip) i++;
		return i;
	}
	
	public int nextIndex(List stuff) {
		return nextInt(stuff.size());
	}

	/**
	 * Pick an index into the given array, weighted according to the values in the array.
	 * An array of 1s will give a uniformly random number from 0..sizes.length-1.
	 * @param sizes
	 * @param total
	 * @return
	 */
	public int pickWeightedIndex(int[] sizes, int total) {
		int result = sizes.length-1;
		final int pick = nextInt(total);
		int acc = 0;
		for (int i = 0 ; i<sizes.length; i++) {
			if (sizes[i] == 0) continue;
			if (acc <= pick && (acc += sizes[i]) > pick) { result = i; break; } 
		}
		if (sizes[result] == 0) {
			System.err.println("Double WTF");
		}
		return result;
	}

	final public int nextCodedVertex(Solution solution) {
		final int offset = solution.size() - 1;
		//number from -(solution.size()-1) ... vertex count - 1
		// is a number from zero to vertex count - 1 + solution.size() - 1
		return nextInt(solution.getVertexCount() + offset) - offset;
	}
	
	final public int nextCustomer(Solution solution) {
		return nextInt(solution.getVertexCount()-1) + 1;
	}
	/**
	 * Uniformly randomly select a member of the given array of integers
	 * @param neighbours
	 * @return
	 */
	public int pick(int[] neighbours) {
		return neighbours[nextInt(neighbours.length)];
	}
	public static void main(String[] args) {
		int[] sizes = {10, 0, 10, 0};
		HintonRandom random = new HintonRandom();
		while (true) {
			int ix = random.pickWeightedIndex(sizes, 20);
			if (ix == 1) {
				System.err.println("DIE");
			}
		}
	}
}
