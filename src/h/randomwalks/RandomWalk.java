package h.randomwalks;

import h.options.IntegerParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.NothingParser;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.util.random.HintonRandom;
import h.util.timing.Timer;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.Instance.ConstraintMode;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.IMove;
import h.vrp.search.INeighbourhood;
import h.vrp.search.util.NeighbourhoodSpecParser;
import h.vrp.solcons.RandomConstructor;

public class RandomWalk {
	public float[] values;
	public float[] lagAC;
	
	public RandomWalk(Instance instance, Solution start, INeighbourhood neighbourhood, int length, boolean feasibleLandscape, boolean killZeros) {
		values = new float[length];
		
		DeltaEvaluator de = new DeltaEvaluator(instance, start, 
				feasibleLandscape ? ConstraintMode.HARD_CONSTRAINTS :
									ConstraintMode.NO_CONSTRAINTS);
		
		if (!de.isFeasible()) {
			System.err.println("This shouldn't happen! walk starting at an infeasible point!");
		}
		
//		int zeros = 0, infeasibles = 0;
		
		float currentCost = de.cost();
		for (int i = 0; i<length;) {
			values[i] = currentCost;
			IMove m = neighbourhood.sample(start);
			if (m != null) {
				de.test(m.getChanges());
				final boolean b = de.isFeasible();
				if (b) {
					float nextCost = de.cost();
					if (killZeros && (nextCost == currentCost)) {
						de.reject();
//						zeros++;
					} else {
						start.applyMove(m);
						de.accept();
						i++;
						currentCost = nextCost;
					}
				} else {
					de.reject();
//					infeasibles++;
				}
			}
		}
		
//		System.err.println(infeasibles + " infeasible moves, " + zeros + " identity moves");
	}
	
	/**
	 * Calculate the autocorrelation for lags up to max_s
	 * @param max_s
	 */
	public void calculateAC(int max_s, float mean, float var) {
		final int n = values.length;
		lagAC = new float[max_s];
//		/**
//		 * The expectation over t of psi
//		 */
//		float sampleMean = 0;
//		/**
//		 * the expectation over t of psi^2
//		 */
//		float squaredSampleMean = 0;
//		for (float f : values) {
//			sampleMean += f; 
//			squaredSampleMean += Math.pow(f, 2);
//		}
//		sampleMean /= n;
//		squaredSampleMean /= n;
//		/**
//		 * square of expectation of t
//		 */
//		final float sampleMean_squared = (float) Math.pow(sampleMean, 2);
//		
//		final float mean = sampleMean;
//		final float var = 
		

		
		for (int k = 0; k<max_s; k++) {
			float sum = 0;
			for (int t = 0; t<n - k; t++) {
				sum += (values[t] - mean) * (values[t+k] - mean);
			}
			float r_hat = sum/(var * (n-k));
			lagAC[k] = r_hat;
		}
	}
	
	public static void main(String[] args) throws InvalidOptionException, InvalidArgumentException {
		Options options = new Options("--");
		InstanceOption.addTo(options);
		options.addOption("moves", "Move type", new StringParser("(random)"));
		options.addOption("length", new IntegerParser(1000));
		options.addOption("max-s", new IntegerParser(400));
		options.addOption("repeats", new IntegerParser(10));
		options.addOption("feasible-landscape", new NothingParser());
		options.addOption("kill-zeros", new NothingParser());
		options.parse(args);
		
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		HintonRandom hr = new HintonRandom();
		int repeats = options.getOption("repeats");
		int rep = repeats;
		int length = options.getOption("length");
		int max_s = options.getOption("max-s");
		
		boolean killZeros = options.getOption("kill-zeros");
		
		double[][] averageLagAC = new double[max_s][2];
		
//		InstanceEstimator ie = new InstanceEstimator(instance, 100000);
//		System.err.println(ie);
		
		Timer t = Timer.getInstance();
		t.reset();
		
		boolean feasibleLandscape = options.getOption("feasible-landscape");
		if (feasibleLandscape) {
			System.err.println("Will try and sample from the feasible landscape");
			instance.setUsingHardConstraints(true);
		}
		RandomConstructor con = new RandomConstructor(hr, true, feasibleLandscape, 100000);
		int sampler = 100;
		boolean sampling = true;
		while (rep > 0) {
			if (sampling) {
				if (sampler == 0) {
					sampling = false;
					double t200 = t.getElapsedTime();
					t200 /= 100;
					t200 *= rep;
					System.err.println("This will take about " + t200 + " more seconds");
					System.err.println("That's about " + t200/60+ " mins or " + t200/3600 + " hrs");
				}
				sampler--;
			}
			
			Solution start = con.createSolution(instance);
			if (start == null) {
				String i = "    ";
				String n = "\n";
				String c = ",";
				StringBuilder output = new StringBuilder();
				output.append("RW(");
				output.append("instance=\"" + instance + "\"" + c + n);
				output.append(i + "feasible=True, no_start=True)" + n);
				System.out.println(output);
				return;
			}
			INeighbourhood n = NeighbourhoodSpecParser.parseSexp((String) options.getOption("moves"), 
					instance, start, hr);
			
			
			RandomWalk walk = new RandomWalk(instance, start, n, length, feasibleLandscape, killZeros);
			walk.calculateAC(max_s);//, (float) ie.getSampleMean(), (float) ie.getVarianceEstimator());
			
//			System.out.println(Arrays.toString(walk.lagAC));
			
			for (int i = 0; i<averageLagAC.length; i++) {
				averageLagAC[i][0] += walk.lagAC[i];
				averageLagAC[i][1] += Math.pow(walk.lagAC[i], 2);
			}
			
			rep--;
		}
		
		for (int i = 0; i<averageLagAC.length; i++) {
			averageLagAC[i][0] /= repeats;
			averageLagAC[i][1] /= repeats;
			averageLagAC[i][1] -= Math.pow(averageLagAC[i][0], 2);
		}
//		for (double[] d : averageLagAC) {
//			System.out.print(d[0] + ", ");//"(" + d[0] + ", " + Math.sqrt(d[1]) + ") ");
//		}
//		
//		System.out.println();
//		final double l_twiddle = -1/Math.log(averageLagAC[1][0]);
//		
//		//perform exponential regression
//		for (int i = 0; i<averageLagAC.length; i++) {
//			System.out.print(averageLagAC[i][0] - Math.exp(-i * l_twiddle) + " ");
//		}
		String i = "    ";
		String n = "\n";
		String c = ",";
		StringBuilder output = new StringBuilder();
		output.append("RW(");
		output.append("instance=\"" + instance + "\"" + c + n);
		output.append(i + "feasible=" + (feasibleLandscape ? "True" : "False") + c + n);
		output.append(i + "moves=\"" + (String) options.getOption("moves") + "\"" + c + n);
		output.append(i + "repeats=" + repeats + c + n);
		output.append(i + "length=" + length + c + n);
		
		output.append(i+"corr_length=" + (-1/Math.log(averageLagAC[1][0])) + c + n);
		
		output.append(i + "average_lag=[");
		for (int j = 0; j<averageLagAC.length; j++) {
			if (j != 0) output.append(", "); 
			output.append(averageLagAC[j][0]);
		}
		output.append("]" +c+ n);
		output.append(i + "variance=[");
		for (int j = 0; j<averageLagAC.length; j++) {
			if (j != 0) output.append(", "); 
			output.append(averageLagAC[j][1]);
		}
		output.append("])"+ n);
		System.out.println(output);
	}

	public void calculateAC(int maxS) {
		Stats s = new Stats();
		for (float f : values) {
			s.addSample(f);
		}
		s.update();
		calculateAC(maxS, (float)s.getSampleMean(), (float)s.getVarianceEstimator());
	}
}
