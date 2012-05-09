package h.randomwalks;

import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.Instance.ConstraintMode;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.solcons.RandomConstructor;

public class InstanceEstimator {
	private Stats delegate;

	public InstanceEstimator(Instance instance, int sampleSize) {
		delegate = new Stats();
	
		HintonRandom r = new HintonRandom();
		RandomConstructor sc = new RandomConstructor(r, true, false);
		
		for (int i = 0; i<sampleSize; i++) {
			Solution s = sc.createSolution(instance);
			DeltaEvaluator de = new DeltaEvaluator(instance, s, ConstraintMode.NO_CONSTRAINTS);
			delegate.addSample(de.cost());
		}

		delegate.update();
	}

	public double getSampleMean() {
		return delegate.getSampleMean();
	}

	public double getSEM() {
		return delegate.getSEM();
	}

	public double getVarianceEstimator() {
		return delegate.getVarianceEstimator();
	}

	public String toString() {
		return delegate.toString();
	}
}
