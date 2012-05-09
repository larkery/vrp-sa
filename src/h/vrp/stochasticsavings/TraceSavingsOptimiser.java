package h.vrp.stochasticsavings;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.util.timing.Timer;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.solcons.SavingsConstructor;

import java.util.Random;

public class TraceSavingsOptimiser {

	private static final String O_RUNTIME = "runtime";
	private static final String O_MODE = "mode";
	private static final String O_PARAMETER = "stochasticity";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options options = new Options("--");
		
		InstanceOption.addTo(options);
		
		options.addOption(O_RUNTIME, "Running time in seconds", new IntegerParser(3600));
		options.addOption(O_MODE, "Mode to test", new StringParser("h"));
		options.addOption(O_PARAMETER, "Randomness parameter", new DoubleParser(0.75));
		
		try {
			options.parse(args);
		} catch (Exception ex) {
			System.err.println("Bad option somewhere; " + ex.getMessage());
			ex.printStackTrace();
		}
		//load the instance
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		
		int runtime = options.getOption(O_RUNTIME);
		String mode = options.getOption(O_MODE);
		final double parameter = options.getOption(O_PARAMETER);
		
		// want:
		// distribution information
		// quality deltas
		
		SavingsConstructor normalSavings = new SavingsConstructor();
		Solution solution = normalSavings.createSolution(instance);
		final float cw_baseline = evaluateSolution(instance, solution);
		
		Histogram h = new Histogram(-100, 100, 0.1);
		
		Random random = new Random();
		
		SavingsConstructor randomisedSavings = null;
		if (mode.equals("h")) {
			randomisedSavings = new SavingsConstructor(ExtendedSavingsOptimiser.getRandomisedSelector(random, parameter));
		} else if (mode.equals("g")) {
			randomisedSavings = new SavingsConstructor(ExtendedSavingsOptimiser.getRandomisedCalculator(SavingsConstructor.normalSavingsCalculator, random, parameter));
		}
		//now we have constructed a randomised savings device of some description, run zillions of tests
		
		Timer t = Timer.getInstance();
		t.reset();
		while (t.getElapsedTime() < runtime) {
			solution = randomisedSavings.createSolution(instance);
			final float sc = evaluateSolution(instance, solution);
			
			h.add(100 * (sc - cw_baseline) / cw_baseline);
		}
		System.out.println("SavingsDistribution(");
		System.out.println(indent("parameters=\n" + indent(options.toString()) + ","));
		System.out.println(indent("reference="+cw_baseline+","));
		System.out.println(indent("histogram="+h+")"));
	}
	
	final static float evaluateSolution(Instance i, Solution s) {
		DeltaEvaluator de = new DeltaEvaluator(i, s);
		return de.cost();
	}
	private static String indent(String str) {
		return "    " + str.replace("\n", "\n    ");
	}
}
