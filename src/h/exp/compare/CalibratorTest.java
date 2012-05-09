package h.exp.compare;

import h.options.DoubleParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.NothingParser;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.util.NeighbourhoodSpecParser;
import h.vrp.solcons.RandomConstructor;

public class CalibratorTest {

	private static final String O_CALIBRATION_MOVES = "moves";
	private static final String O_FEASIBLE = "feasible";
	private static final String O_ALPHA = "alpha";

	/**
	 * @param args
	 * @throws InvalidArgumentException 
	 * @throws InvalidOptionException 
	 */
	public static void main(String[] args) throws InvalidOptionException, InvalidArgumentException {
		Options options = new Options("--");
		options.addOption(O_CALIBRATION_MOVES, new StringParser("(random)"));
		options.addOption(O_FEASIBLE, new NothingParser());
		options.addOption(O_ALPHA, new DoubleParser(0.75));
		InstanceOption.addTo(options);

		options.parse(args);
		
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		instance.setUsingHardConstraints(true);
		HintonRandom random = new HintonRandom();
		
		System.err.println("Estimating T0, calibrating against " + options.getOption(O_CALIBRATION_MOVES));
		
		float[] transitions = new float[70000];
		
		int ticks = transitions.length / 10;
		int ticks2 = ticks;
		double tt = 0;
		
		boolean feasible = options.getOption(O_FEASIBLE);
//		int deadOnes = 0;
		for (int i = 0; i<transitions.length; i++) {
			ticks2--;
			if (ticks2 == 0) {
				System.err.print(i / (float) transitions.length * 100 +"% ");
				ticks2 = ticks;
			}
			Solution randomSolution = new RandomConstructor(random, true, feasible).createSolution(instance);
			if (randomSolution == null) {
				System.out.println(instance + ", " + feasible + "n/a, n/a");
				return;
			}
			INeighbourhood n = NeighbourhoodSpecParser.parseSexp((String)options.getOption(O_CALIBRATION_MOVES), instance, randomSolution, random);
//			INeighbourhood n = new BlindNeighbourhood(instance, randomSolution, random);
			DeltaEvaluator e = new DeltaEvaluator(instance, randomSolution);
			float c1 = e.cost();
			IMove m = null;
			float delta = 0;
			while (true) {
				m = n.sample(randomSolution);
				if (m != null) {
					e.test(m.getChanges());
					float c2 = e.cost();
					delta = c2 - c1;
					if (delta < 0) delta *=-1;
					if (delta != 0 && (!feasible || e.isFeasible())) {
//						if (!e.isFeasible()) deadOnes++;
						break;
					} else {
						e.reject();
					}
				}
			}
			transitions[i] = delta;
			tt+=delta;
		}
//		System.err.println("Dead ones: " + deadOnes);
		System.err.println("Avg delta :  " +tt/ transitions.length);
		float tmax = 1;

		double desiredAr = options.getOption(O_ALPHA);
		float ar = calculateAcceptance(transitions, tmax);
		ticks = 0;
		while (Math.abs(ar - desiredAr) > 0.001) {
			if (ar < desiredAr) tmax *=1.2;
			else tmax /=1.3;
			System.err.print(ar + " ("+tmax + ") ");
			ar = calculateAcceptance(transitions, tmax);
			ticks++;
			if (ticks > 50) {
				System.err.println("Failed, setting tmax to 50");
				tmax = 50;
				break;
			}
		}
		
		double meanDelta = 0;
		
		System.err.println();
		System.err.println("T = " +  tmax + " gives " + (ar * 100) + "% acceptance");
		System.out.println(instance + ", " + ((String) options.getOption(O_CALIBRATION_MOVES)) + ", " + feasible + ", " + tmax + ", " + tt / transitions.length);
	}
	private static float calculateAcceptance(float[] transitions, float tmax) {
		double accepts = 0;
		long cases = 0;
		for (float delta : transitions) {
			if (delta > 0) {
				accepts += Math.exp(-delta/tmax);
				cases++;
			}
		}
		return (float) (accepts / cases);
	}
}
