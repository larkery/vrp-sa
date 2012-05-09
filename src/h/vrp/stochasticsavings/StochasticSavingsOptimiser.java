package h.vrp.stochasticsavings;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.InvalidArgumentException;
import h.options.NothingParser;
import h.options.OptionParser;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.sexp.GeneralSexpParser;
import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.IFitnessListener;
import h.vrp.search.IOptimiser;
import h.vrp.solcons.ExtendedSavingsCalculator;
import h.vrp.solcons.SavingsConstructor;
import h.vrp.solcons.SavingsConstructor.IMoveSelector;
import h.vrp.solcons.SavingsConstructor.ISavingsCalculator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StochasticSavingsOptimiser implements IOptimiser {
	public enum Mode {
		DETERMINISTIC, BERNOULLI, WINDOW, UNIFORM, GAUSSIAN, ALTINEL
	}

	private int repeats;
	private Instance instance;
	private double parameter;
	private Mode mode;
	private int repeat;
	private CrossIterator<Double> weightIterator;
	private double lambda;
	private double mu;
	private double nu;
	
	private IMoveSelector selector;
	private Random random;
	
	Solution bestSolution;
	
	float bestFitness;
	double bestParameters[];
	private Histogram histogram;
	private float baseline;
	private float[][] bitmap; 
	                
	public StochasticSavingsOptimiser(Mode m, double parameter, Instance instance, int repeats, List<List<Double>> weights) {
		this.mode = m;
		this.parameter = parameter;
		this.instance = instance;
		this.repeats = repeats;
		this.repeat = 0;
		this.weightIterator = new CrossIterator<Double>(weights);
		this.bestParameters = new double[3];
		this.bestFitness = Float.MAX_VALUE;
		this.random = new Random();
		
		this.histogram = new Histogram(-2, 1, 0.005);
		
		this.baseline = new DeltaEvaluator(instance,(new SavingsConstructor()).createSolution(instance)).cost();
		
		int weightCount = 1;
		for (List<Double> d : weights) weightCount *= d.size();
		
		switch (mode) {
		case DETERMINISTIC:
			bitmap = new float[weightCount][4];
		case UNIFORM:
		case GAUSSIAN:
			selector = SavingsConstructor.normalMoveSelector;
			break;
		case BERNOULLI:
			selector = StochasticComponents.getBernoulliSelector(random, parameter);
			break;
		case WINDOW:
			selector = StochasticComponents.getWindowSelector(random, (int) parameter);
			break;
		}
	}
	
	
	@Override
	public void setFitnessListener(IFitnessListener trace) {

	}

	@Override
	public boolean step() {
		float[] bit = null;
		if (repeat == 0 && weightIterator.hasNext()) {
			weightIterator.next();
			this.lambda = weightIterator.get(0);
			this.mu = weightIterator.get(1);
			this.nu = weightIterator.get(2);
			if (bitmap != null) {
				bit = bitmap[weightIterator.position()];
				bit[0] = (float) lambda;
				bit[1] = (float) mu;
				bit[2] = (float) nu;
			}
		}
		
		ISavingsCalculator calc = ExtendedSavingsCalculator.createInstance(lambda, mu, nu);

		switch (mode) {
		case UNIFORM:
			calc = StochasticComponents.getUniformlyPerturbingCalculator(calc, random, parameter);
			break;
		case GAUSSIAN:
			calc = StochasticComponents.getGaussianPerturbingCalculator(calc, random, parameter);
			break;
		}
		
		SavingsConstructor sc = new SavingsConstructor(selector, calc);
		Solution sol = sc.createSolution(instance);
		DeltaEvaluator evaluator = new DeltaEvaluator(instance, sol);
		final float cost = evaluator.cost();
		
		histogram.add((baseline - cost) / baseline);
		if (bit != null) {
			bit[3] = ((baseline - cost) / baseline);
		}
		if (cost < bestFitness) {
			bestSolution = sol;
			bestFitness = evaluator.cost();
			bestParameters[0] = lambda;
			bestParameters[1] = mu;
			bestParameters[2] = nu;
		}
		
		repeat++;
		if (repeat >= repeats) {
			repeat = 0;
			return weightIterator.hasNext();
		} else {
			return true;
		}
	}

	static final String O_MODE = "mode";
	static final String O_PLUS = "with-altinel";
	
	static final String O_LAMBDA = "lambda";
	static final String O_MU = "mu";
	static final String O_NU = "nu";
	
	static final String O_PARAMETER = "randomness";
	static final String O_REPEATS = "repeats";
	
	static final String ALTINEL_LAMBDA = "(range 0.1 2 0.1)";
	static final String ALTINEL_MU = "(range 0 2 0.1)";
	static final String ALTINEL_NU = "(range 0 2 0.1)";
	private static final String O_WEIGHTS_CACHE = "best_altinel_parameters.csv";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options options = new Options("--");
		
		InstanceOption.addTo(options);
		
		options.addOption(O_MODE, "The mode to use; <DETERMINISTIC | ALTINEL | UNIFORM | GAUSSIAN | BERNOULLI | WINDOW>", 
				new OptionParser() {
					@Override
					public Object parse(String op, Iterator<String> iter)
							throws InvalidArgumentException {
						String s = iter.next();
						for (Mode m : Mode.values()) {
							if (m.name().equals(s)) {
								return m;
							}
						}
						throw new InvalidArgumentException("No such mode: " + s);						
					}
					
					@Override
					public boolean hasDefaultValue() {
						return true;
					}
					
					@Override
					public Object getDefaultValue() {
						return Mode.DETERMINISTIC;
					}
			});
		
		options.addOption(O_PLUS, "Use altinel's method to find weights before running other method", new NothingParser());
		options.addOption(O_LAMBDA, "Lambda values", new StringParser("(list 1.0)"));
		options.addOption(O_MU, "Mu values", new StringParser("(list 0.0)"));
		options.addOption(O_NU, "Nu values", new StringParser("(list 0.0)"));
		
		options.addOption(O_REPEATS, "Random mode repeats", new IntegerParser(8820));
		
		options.addOption(O_PARAMETER, "Random mode parameter", new DoubleParser(0));
		
		/// Now parse arguments
		try {
			options.parse(args);
		} catch (Exception ex) {
			System.err.println(options.getHelp());
			System.err.println("Now the error:" + ex);
			ex.printStackTrace();
		}
		
		List<List<Double>> weights = new ArrayList<List<Double>>();
		ISexpEvaluator weightEvaluator = new GeneralSexpParser();
		
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		
		Mode m = options.getOption(O_MODE);
		int repeats = options.getOption(O_REPEATS);
		switch (m) {
		case ALTINEL:
			m = Mode.DETERMINISTIC;
			
			weights.add((List<Double>) SexpParser.evaluateString(ALTINEL_LAMBDA, weightEvaluator));
			weights.add((List<Double>) SexpParser.evaluateString(ALTINEL_MU, weightEvaluator));
			weights.add((List<Double>) SexpParser.evaluateString(ALTINEL_NU, weightEvaluator));
			break;
		default:
			if (options.getOption(O_PLUS)) {
				double[] t = cacheLookup(instance.toString());
				if (t == null) {
					System.err.println("Finding and caching altinel parameters");
					List<List<Double>> defaultWeights = new ArrayList<List<Double>>();
					
					defaultWeights.add((List<Double>) SexpParser.evaluateString(ALTINEL_LAMBDA, weightEvaluator));
					defaultWeights.add((List<Double>) SexpParser.evaluateString(ALTINEL_MU, weightEvaluator));
					defaultWeights.add((List<Double>) SexpParser.evaluateString(ALTINEL_NU, weightEvaluator));
				
					StochasticSavingsOptimiser sso = new StochasticSavingsOptimiser(Mode.DETERMINISTIC, 0, instance, 1, defaultWeights);
					while (sso.step());
					t = sso.bestParameters;
					addToCache(instance.toString(), t, sso.bestFitness);
					System.err.println("Best parameters:" + Arrays.toString(t) + ", " + sso.bestFitness);
				} else {
					System.err.println("Got parameters from cache:" + Arrays.toString(t));
				}
				for (int i = 0; i<3; i++) weights.add(Collections.singletonList(t[i]));
			} else {
				weights.add((List<Double>) SexpParser.evaluateString((String) options.getOption(O_LAMBDA), weightEvaluator));
				weights.add((List<Double>) SexpParser.evaluateString((String) options.getOption(O_MU), weightEvaluator));
				weights.add((List<Double>) SexpParser.evaluateString((String) options.getOption(O_NU), weightEvaluator));
			}
			break;
		}
		
		if (m == Mode.DETERMINISTIC) {
			if (repeats > 1) {
				System.err.println("Setting repeats to 1");
				repeats = 1;
			}
		}
		double parameter = options.getOption(O_PARAMETER);
//		System.err.println("Constructing...");
		StochasticSavingsOptimiser sso = new StochasticSavingsOptimiser(m, parameter, instance, repeats, weights);
//		System.err.println("Done Constructing...");
		
		int ticks = 1;
		while (sso.step()) {
			ticks++; //and we're done.
		}
		System.err.println(ticks + " tests");
//		System.out.println(sso.bestFitness);
		
		if (options.getOption(O_MODE).equals(Mode.ALTINEL)) {
			if (cacheLookup(instance.toString()) == null) {
				System.err.println("Adding parameters for " + instance + " to cache");
				addToCache(instance.toString(), sso.bestParameters, sso.bestFitness);
			}
		}
		
		StringBuilder solutionDesc = new StringBuilder();
		for (Route r : sso.bestSolution) {
			if (r.size() == 1) continue;
			if (solutionDesc.length() == 0) {
				solutionDesc.append("[");
			} else {
				solutionDesc.append(", ");
			}
			solutionDesc.append("[");
			for (int i = 0; i<r.size(); i++) {
				if (i != 0) solutionDesc.append(", ");
				solutionDesc.append(r.get(i));
			}
			solutionDesc.append("]");        
		}
		solutionDesc.append("]");
		
		Solution baseline = (new SavingsConstructor()).createSolution(instance);
		int[] differences = new int[instance.getPoints().size()];
		for (int i = 1; i<instance.getPoints().size(); i++) {
			for (int j = 1; j<i; j++) {
				if (j == i) continue;
				if (baseline.routeContaining(i) == baseline.routeContaining(j)) {
					if (sso.bestSolution.routeContaining(i) != sso.bestSolution.routeContaining(j)) {
						differences[i]++;
					}
				}
			}
		}
		
		int mc = 0;
		for (int i = 1; i<differences.length; i++) {
			if (differences[i] > baseline.get(baseline.routeContaining(i)).size()/2) {
				mc++;
			}
		}
		
		instance.setUsingHardConstraints(true);
		Map<String, List<Float>> slack = new DeltaEvaluator(instance, sso.bestSolution).getSlacknesses();
	
		System.out.println("SR(");
		System.out.println("    instance=\"" + instance.toString() + "\", mode=\"" + options.getOption(O_MODE) + "\", plus=" + (options.getOption(O_PLUS) ? "True" : "False") + ", fitness=" + sso.bestFitness +
				", weights=" + Arrays.toString(sso.bestParameters) + ", parameter=" + parameter + ", "+ "repeats=" + repeats + ", "+
				"\n    moved_vertices=" + mc +",");
		
		for (Map.Entry<String, List<Float>> e : slack.entrySet()) {
			float t = 0;
			for (float f : e.getValue()) t+=f;
			t/= e.getValue().size();
			System.out.println("    slack_" + e.getKey() + "=" + t + ",");
		}
		
		if (options.getOption(O_MODE).equals(Mode.ALTINEL)) {
			float[][] bitmap = sso.bitmap;
			StringBuilder sb = new StringBuilder();
			for (float[] f : bitmap) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(Arrays.toString(f));
			}
			System.out.println("    bitmap=[" + sb.toString() + "],");
		}
		
		System.out.println("    histogram=" + sso.histogram + "," +
				"\n    solution=" + solutionDesc.toString() + 
		")");
	}

	private static double[] cacheLookup(String instance) {
		try {
			File f = new File(O_WEIGHTS_CACHE);
			if (f.exists()) {
				BufferedReader fis = new BufferedReader(new FileReader(f));
				String l;
				while ((l = fis.readLine()) != null) {
					String[] s = l.split(",");
				
					if (s[0].equals(instance.toString())) {
						return new double[] {
							Double.parseDouble(s[1]),
							Double.parseDouble(s[2]),
							Double.parseDouble(s[3])
						};
					}
				}
			}
		} catch (Exception e) {}
		return null;
	}
	
	private static void addToCache(String instance, double[] values, float bf) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(O_WEIGHTS_CACHE, true));
			bw.write(instance.toString() + ", " + values[0] + ", " + values[1] + ", " + values[2] + ", " + bf);
			bw.newLine();
			bw.close();
		} catch (Exception e) {}
	}
	
	class CrossIterator<T> {
		private List<List<T>> ranges;
		private List<Iterator<T>> iters;
		private List<T> values;
		int position = -1;
		boolean hasNext = true ;
		public CrossIterator(List<List<T>> ranges) {
			this.ranges = ranges;
			this.iters = new ArrayList<Iterator<T>>();
			this.values = new ArrayList<T>();
			for (List<T> l : ranges) {
				Iterator<T> it = l.iterator();
				iters.add(it);
				hasNext = hasNext && it.hasNext();
				values.add(l.get(0));
			}
			for (int i = 1; i<iters.size(); i++) {
				values.set(i, iters.get(i).next());
			}
		}
		public int position() {
			return position;
		}
		public boolean hasNext() {
			return hasNext;
		}
		
		public void next() {
			int iter = 0;
			while (!iters.get(iter).hasNext()) {
				iters.set(iter, ranges.get(iter).iterator()); //reset this iterator
				values.set(iter, iters.get(iter).next()); //reset corresponding value
				
				iter++;
			}

			values.set(iter, iters.get(iter).next()); //bump the value which needs bumping
			hasNext = false;
			for (Iterator<T> i: iters) {
				hasNext = hasNext || i.hasNext();
			}
			position++;
		}
		public T get(int index) {
			return values.get(index);
		}
	}

	@Override
	public boolean finished() {
		return false;
	}
}
