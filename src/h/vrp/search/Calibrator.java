package h.vrp.search;

import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.solcons.RandomConstructor;

public class Calibrator {
	float[] transitions;
	
	public Calibrator(Instance instance, INeighbourhoodConstructor nc, HintonRandom random, int sampleSize) {
		transitions = new float[sampleSize];
		RandomConstructor rc = new RandomConstructor(random, true, false);
		for (int i = 0; i<transitions.length; i++) {
			Solution rs = rc.createSolution(instance);
			INeighbourhood n = nc.createNeighbourhood(instance, rs);
			DeltaEvaluator e = new DeltaEvaluator(instance, rs);
			float c1 = e.cost();
			IMove m = null;
			float delta = 0;
			while (true) {
				m = n.sample(rs);
				if (m != null) {
					e.test(m.getChanges());
					float c2=e.cost();
					delta = Math.abs(c2 - c1);
					if (delta == 0)
						e.reject();
					else
						break;
				}
			}
			transitions[i] = delta;
		}
	}
	
	public float calculateTemperature(float desiredAcceptance, float error) {
		float T = 1;
		float ar = calculateAcceptance(T);
		
		int bail = 100;
		
		while (Math.abs(ar - desiredAcceptance) > error) {
			if (ar < desiredAcceptance) T *= 1.2;
			else T/= 1.3;
			ar = calculateAcceptance(T);
			bail--;
			if (bail == 0) {
				System.err.println("Could not converge on a temperature! You get 50.");
				return 50;
			}
		}
		
		return T;
	}
	
	public float calculateAcceptance(float temperature) {
		double accepts = 0;
		for (float d : transitions) {
			accepts += Math.exp(-d / temperature);
		}
		return (float) accepts / transitions.length;
	}
}
