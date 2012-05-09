package h.vrp.search;

import h.vrp.model.Solution;
/**
 * Defines a neighbourhood operator.
 * 
 * @author hinton
 */
public interface INeighbourhood {
	/**
	 * Randomly get a neighbour of the given solution from this neighbourhood
	 * @param solution
	 * @return an {@link IMove} which will transform the solution.
	 */
	IMove sample(Solution solution);
	
	int size(Solution solution);
}
