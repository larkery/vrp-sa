package h.exp.compare;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.NothingParser;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.sexp.ArithmeticEvaluator;
import h.util.random.HintonRandom;
import h.util.timing.Timer;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.Annealer;
import h.vrp.search.Calibrator;
import h.vrp.search.IFitnessListener;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.INeighbourhoodConstructor;
import h.vrp.search.IOptimiser;
import h.vrp.search.util.NeighbourhoodSpecParser;
import h.vrp.solcons.ISolutionConstructor;
import h.vrp.solcons.RandomConstructor;
import h.vrp.solcons.SavingsConstructor;
import h.vrp.solcons.SolConSpecParser;

import java.awt.geom.Point2D.Double;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * A test which runs an optimiser for a certain amount of time with some parameters
 * and records the results
 */
public class RunOptimiser {

	private static final String O_MOVES = "moves";
	private static final String O_START = "start";
	private static final String O_SEED = "seed";
	
	
//	private static final String O_SOURCE = "source";
	private static final String O_INFEASIBILITY_PENALTY = "penalty";
//	private static final String O_CONTROL_MOVES = "control";
	private static final String O_T0 = "T0";
	private static final String O_REPEATS = "repeats";
	private static final String O_SOFT_CONSTRAINTS = "softconstraints";
	private static final String O_ALPHA = "cooling";
	private static final String O_EPOCH_LENGTH = "epoch_length";

	
	private static final String O_CALIBRATION_MOVES = "calibration";

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) {
		Options options = new Options("--");
		
		InstanceOption.addTo(options);
		
//		options.addOption(O_SOURCE, "The source of the instance being solved; (file filename) or (random size cap dist)", 
//				new StringParser("(random 100 10 10)"));
		
		options.addOption(O_T0, "Initial temperature T0 (if not given, determine T0 automatically)", new DoubleParser());
		options.addOption(O_ALPHA, "Cooling rate", new DoubleParser(0.95));
		options.addOption(O_EPOCH_LENGTH, "Epoch length (moves / epoch)", new StringParser("(* 15 n (- n 1))"));
		
		options.addOption(O_SEED, "Random seed", new IntegerParser());
		options.addOption(O_START, "How to create the start solution; (savings) => clarke-wright method, (random fullrandom feasible) => random solution",
				new StringParser("(savings)"));		
		
		options.addOption(O_CALIBRATION_MOVES, "Calibration moves to use when calculating T0", new StringParser("(random)"));
		
		options.addOption(O_MOVES,
				"neighbourhood to use; sexp format, read the code",
				new StringParser("(random)"));
		
		options.addOption(O_INFEASIBILITY_PENALTY, "Infeasibility penalty (how much each unit of infeasibility costs)",
				new IntegerParser(1000));
		
		options.addOption(O_SOFT_CONSTRAINTS, 
				"Whether to disable hard constraints and just use the penalty functions",
				new NothingParser());
		
		options.addOption(O_REPEATS, "Number of repeat test runs", new IntegerParser(2));
		
		try {
			options.parse(args);
			Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
//			Instance instance = SourceSpecParser.parseSexp((String) options.getOption(O_SOURCE)).createInstance(); 
			
			instance.setInfeasibilityPenalty((Integer) options.getOption(O_INFEASIBILITY_PENALTY));
			instance.setUsingHardConstraints(!((Boolean)options.getOption(O_SOFT_CONSTRAINTS)));
			System.err.println("Constraints " + (instance.isUsingHardConstraints() ? "on" : "off"));
			HintonRandom random;
			if (options.hasOption(O_SEED)) {
				random = new HintonRandom((Integer)options.getOption(O_SEED));
			} else {
				random = new HintonRandom();
			}
			System.err.println("Constructing solution...");
			ISolutionConstructor sc = SolConSpecParser.parseSpec((String) options.getOption(O_START), random);
			Solution solution = sc.createSolution(instance);
			
//			TraceCollector tests = new TraceCollector();
			float tmin = 0.05f;
			int repeats = (Integer) options.getOption(O_REPEATS);
			
			int epochLength = 
				(int) ArithmeticEvaluator.evaluateSexp((String) options.getOption(O_EPOCH_LENGTH),
				new Object[]{"n", instance.getPoints().size()}	
				);
			System.err.println("Epoch length = " + epochLength);
			
			
			float tmax;
			if (options.hasOption(O_T0)) {
				tmax = ((java.lang.Double)options.getOption(O_T0)).floatValue();
			} else {
				System.err.println("Estimating T0, calibrating against " + options.getOption(O_CALIBRATION_MOVES));
				final String calMoves = options.getOption(O_CALIBRATION_MOVES);
				final HintonRandom r2 = random;
				Calibrator cal = new Calibrator(instance,
						new INeighbourhoodConstructor() {
							@Override
							public INeighbourhood createNeighbourhood(Instance i, Solution s) {
								return NeighbourhoodSpecParser.parseSexp(calMoves, i, s, r2);
							}
						},
						random,
						50000);
						
				tmax = cal.calculateTemperature(0.75f, 0.01f);
				tmin = cal.calculateTemperature(0.01f, 0.0001f);
				
				System.err.println("T0=" + tmax + ", Tmin=" + tmin);
			}
			
			System.out.println("Result(");
			String parameters = options.toString();
			if (sc instanceof SavingsConstructor) {
				DeltaEvaluator evaluator = new DeltaEvaluator(instance, solution);
				System.out.println(indent("cw_cost=" + evaluator.cost() + ","));				
			} else {
				Solution savingsSolution = (new SavingsConstructor()).createSolution(instance);
				DeltaEvaluator evaluator = new DeltaEvaluator(instance, savingsSolution);
				System.out.println(indent("cw_cost=" + evaluator.cost() + ","));
			}
			
			System.out.println(indent("parameters=\n" + indent(parameters) +","));
			System.out.println("#PREEMPT");
			double[] values = new double[repeats];
			double[] times = new double[repeats];
						
			final double[] min = new double[1];
			IFitnessListener fl = new IFitnessListener() {
				@Override
				public void fitnessChanged(IOptimiser source, Solution state,
						float newFitness, long ticks) {
					if (min[0] > newFitness)
						min[0] = newFitness;
				}
			};
			for (int i = 0; i<repeats; i++) {
				System.err.println("Running repeat " + (i+1) + "/" + repeats);
				Solution clonedSolution = (Solution) solution.clone();
				INeighbourhood neighbourhood = NeighbourhoodSpecParser.parseSexp((String)options.getOption(O_MOVES), instance, clonedSolution, random);
				
				Annealer a = new Annealer(instance, clonedSolution, neighbourhood, epochLength, tmax, 
						(java.lang.Double) options.getOption(O_ALPHA), random);
				min[0] = java.lang.Double.MAX_VALUE;
				a.setFitnessListener(fl);
				Timer t = Timer.getInstance();
				t.reset();
				while (a.getTemperature() > tmin) a.step();
				values[i] = min[0];
				times[i] = t.getElapsedTime();
//				tests.collectTrace(a);
			}
			
			System.out.println(indent("values=" + Arrays.toString(values) + ","));
			System.out.println(indent("times=" + Arrays.toString(times) + ")"));
			
//			List<List<Double>> rawTests = tests.rawTimeSequences(1000);
//			List<List<Double>> rawTicks = tests.rawTickSequences(1000);
//			
//			List<Double> meanTestPoints = tests.processTimeSequence(1000, TraceProcessor.mean);
//			List<Double> minTestPoints = tests.processTimeSequence(1000, TraceProcessor.min);
//			
//			List<Double> meanTestTicks = tests.processTickSequence(1000, TraceProcessor.mean);
//			List<Double> minTestTicks = tests.processTickSequence(1000, TraceProcessor.min);
			
//			System.out.println("    raw_times=[");
//			
//			for (int i = 0; i<rawTests.size();i++) {
//				System.out.println("        " + formatPoints(rawTests.get(i)) + ((i < rawTests.size()-1) ? "," : ""));
//			}
//			
//			System.out.println("    ],");
//			
//			System.out.println("    raw_ticks=[");
//			
//			for (int i = 0; i<rawTicks.size();i++) {
//				System.out.println("        " + formatPoints(rawTicks.get(i)) + ((i < rawTicks.size()-1) ? "," : ""));
//			}
//			
//			System.out.println("    ],");
//			
//			System.out.println("    mean_times=" + 
//					formatPoints(meanTestPoints)+ ",");
//			
//			System.out.println("    mean_ticks=" + 
//					formatPoints(meanTestTicks)+ ",");
//			
//			System.out.println("    min_ticks=" + 
//					formatPoints(minTestTicks)+ ",");
//			
//			System.out.println("    min_times=" + 
//					formatPoints(minTestPoints) + ")");
		} catch (Exception e) {
			System.err.println("Problem : " + e);
			System.err.println(options.getHelp());
			e.printStackTrace(System.err);
			return;
		}
	}
	
	private static float calculateTemperature(float[] transitions, double desiredAr) {
		float tmax = 1;
		
		float ar = calculateAcceptance(transitions, tmax);
		int ticks = 0;
		while (Math.abs(ar - desiredAr) > 0.005) {
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
		System.err.println();
		System.err.println("T = " + tmax + " gives " + (ar * 100) + "% acceptance");
		return tmax;
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

	private static String indent(String str) {
		return "    " + str.replace("\n", "\n    ");
	}
	
	private static String formatPoints(List<Double> processTimeSequence) {
		StringBuffer sb = new StringBuffer("[");
		boolean b = false;
		for (Double d : processTimeSequence) {
			if (b) sb.append(", ");
			b = true;
			sb.append("(" + d.x + ", " + d.y + ")");
		}
		sb.append("]");
		return sb.toString();
	}
}
