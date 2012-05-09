package h.vrp.search;

import h.vrp.model.Solution;

import java.util.Random;

/**
 * A neighbourhood which composes several other neighbourhoods, in a weighted probabilistic manner.
 * @author hinton
 *
 */
public class RandomMultiNeighbourhood extends MultiNeighbourhood {
	private Random random;
	private double[] intervals;
	private double total;
	public RandomMultiNeighbourhood(Random random) {
		this.random = random;
		intervals = null;
		total = 0;
	}
	
	/**
	 * Add a new sub-neighbourhood with weight 1;
	 * @param n
	 */
	public void addNeighbourhood(INeighbourhood n) {
		addNeighbourhood(n, 1);
	}
	
	/**
	 * Add a subneighbourhood with a given weight.
	 * @param n
	 * @param proportion
	 */
	public void addNeighbourhood(INeighbourhood n, double proportion) {
		super.addNeighbourhood(n);
		total += proportion;
		double[] temp = intervals;
		intervals = new double[subNeighbourhoods.size()];
		for (int i = 0; i<intervals.length-1;i++)
			intervals[i] = temp[i];
		intervals[intervals.length-1] = total;
	}
	
	@Override
	public IMove sample(Solution solution) {
		double d = random.nextDouble() * total;
		double l = 0;
		for (int i = 0; i<intervals.length; i++) {
			if (l < d && d < intervals[i]) {
				return subNeighbourhoods.get(i).sample(solution);
			}
			l = intervals[i];
		}
		return null;
	}
	public String toString() {
		return subNeighbourhoods.toString();
	}
}
