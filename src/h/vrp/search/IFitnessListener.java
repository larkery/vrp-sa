package h.vrp.search;

import h.vrp.model.Solution;

	public interface IFitnessListener {
		public void fitnessChanged(IOptimiser source, Solution state, float newFitness, long ticks);
	}

