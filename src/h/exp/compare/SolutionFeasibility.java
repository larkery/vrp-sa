package h.exp.compare;

import h.options.IntegerParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.Options;
import h.options.vrp.InstanceOption;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.solcons.ISolutionConstructor;
import h.vrp.solcons.RandomConstructor;

public class SolutionFeasibility {
	private static final String O_SEED = "seed";
	private static final String O_SAMPLES = "samples";
	
	/**
	 * @param args
	 * @throws InvalidArgumentException 
	 * @throws InvalidOptionException 
	 */
	public static void main(String[] args) throws InvalidOptionException, InvalidArgumentException {
		//test solution feasibility
		Options options = new Options("--");
		InstanceOption.addTo(options);
		options.addOption(O_SEED, "Random seed", new IntegerParser());
		options.addOption(O_SAMPLES, "Number of samples", new IntegerParser(100));
		options.parse(args);
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		instance.setUsingHardConstraints(true);
		HintonRandom random = 
			options.hasOption(O_SEED) ?
			new HintonRandom((Integer) options.getOption(O_SEED))
		: new HintonRandom() ;
		ISolutionConstructor solcon = new RandomConstructor(random, true, false);
		int samples = options.getOption(O_SAMPLES);
		int infeasible = 0;
		int feasible = 0;
//		SolutionDistribution sd = SolutionDistribution.getInstance(instance.getPoints().size()-1);
		float best = Float.MAX_VALUE;
		for (int i = 0; i<samples; i++) {
//			int routeCount = sd.pick(random);
//			instance.setVehicleCount(routeCount);
			Solution solution = solcon.createSolution(instance);
			
			DeltaEvaluator evaluator = new DeltaEvaluator(instance, solution);
//			System.err.println(solution);
//			System.err.println(evaluator);
			if (evaluator.isFeasible()) {
				feasible++;
				best = Math.min(best, evaluator.cost());
			} else
				infeasible++;
		}
		
		System.out.println("SF(instance=\"" + instance + "\", feasible=" + feasible + ", infeasible=" + infeasible + ", samples=" + samples + ")");
	}
}
