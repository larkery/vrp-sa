package h.vrp.search.cl;

import java.util.Arrays;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.Options;
import h.options.StringParser;
import h.options.vrp.InstanceOption;
import h.randomwalks.RandomWalk;
import h.sexp.ArithmeticEvaluator;
import h.util.random.HintonRandom;
import h.util.timing.Timer;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.Annealer;
import h.vrp.search.Calibrator;
import h.vrp.search.IFitnessListener;
import h.vrp.search.INeighbourhood;
import h.vrp.search.INeighbourhoodConstructor;
import h.vrp.search.IOptimiser;
import h.vrp.search.util.NeighbourhoodSpecParser;
import h.vrp.solcons.SavingsConstructor;

public class TwoPhaseAnnealer implements IFitnessListener {
	private static final String AR_1 = "ar1";
	private static final String AR_2 = "ar2";
	private static final String FINAL_AR_1 = "final_ar1";
	private static final String FINAL_AR_2 = "final_ar2";
	private static final String N1 = "n1";
	private static final String N2 = "n2";
	private static final String E1 = "epoch1";
	private static final String E2 = "epoch2";
	private static final String COOLING2 = "cooling2";
	private static final String COOLING1 = "cooling1";
	private static final String REPEATS = "repeats";
	
	private HintonRandom random;
	private Phase phase2;
	private Phase phase1;
	private Instance instance;
	private float bestFitness = Float.MAX_VALUE;

	public class Phase {
		INeighbourhoodConstructor nc;
		float initialTemperature;
		float finalTemperature;
		float cooling;
		int epochLength;
		public Phase(INeighbourhoodConstructor nc, float initialTemperature,
				float finalTemperature, float cooling, int epochLength) {
			super();
			this.nc = nc;
			this.initialTemperature = initialTemperature;
			this.finalTemperature = finalTemperature;
			this.cooling = cooling;
			this.epochLength = epochLength;
		}
		public INeighbourhoodConstructor getNc() {
			return nc;
		}
		public float getInitialTemperature() {
			return initialTemperature;
		}
		public float getFinalTemperature() {
			return finalTemperature;
		}
		public float getCooling() {
			return cooling;
		}
		public int getEpochLength() {
			return epochLength;
		}
	}
	
	public TwoPhaseAnnealer(Instance instance, HintonRandom random, Phase p1, Phase p2) {
		this.random = random;
		this.instance = instance;
		this.phase1 = p1;
		this.phase2 = p2;
	}
	
	public TwoPhaseAnnealer() {
		// TODO Auto-generated constructor stub
	}

	public void run() {
		//run phase 1
		Solution solution = new SavingsConstructor().createSolution(instance);
		
		//randomly mangle solution
		RandomWalk walk = new RandomWalk(instance, solution, NeighbourhoodSpecParser.parseSexp("(random)", instance, solution, random),
				1000, true, true);
		walk = null;
		
		INeighbourhood n = phase1.getNc().createNeighbourhood(instance, solution);
		System.err.println("BEGIN PHASE 1");
		Annealer a = new Annealer(instance, solution, 
				n,
				phase1.getEpochLength(),phase1.getInitialTemperature(), phase1.getCooling(), random);
		
		a.setFitnessListener(this);
		
		int sameCounter, lastSize;
		sameCounter = 0;
		lastSize = Integer.MAX_VALUE;
		
		while (a.getTemperature() > phase1.getFinalTemperature() && sameCounter < 50) {
			boolean b = a.step();
			final int k = n.size(solution);
			if (lastSize == k) {
				//System.err.println("Same for " + sameCounter + " (" + k + ")");
				if (b) sameCounter++;
			} else {
//				System.err.println("k = "+ k);
				sameCounter = 0;
			}
			lastSize = k;
			
		}
		n = null;
		System.err.println("BEGIN PHASE 2");
		a = new Annealer(instance, solution, 
				phase2.getNc().createNeighbourhood(instance, solution),
				phase2.getEpochLength(),phase2.getInitialTemperature(), phase2.getCooling(), random);
		a.setFitnessListener(this);
		while (a.getTemperature() > phase2.getFinalTemperature())
			a.step();
	}

	public static void main(String[] args) throws InvalidOptionException, InvalidArgumentException {
		Options options = new Options("--");
		
		InstanceOption.addTo(options);
		
		options.addOption(COOLING1, "Cooling 1", new DoubleParser(0.9));
		options.addOption(COOLING2, "Cooling 2", new DoubleParser(0.99));
		
		options.addOption(AR_1, "First ar", new DoubleParser(0.4));
		options.addOption(FINAL_AR_1, "First end ar", new DoubleParser(0.1));
		
		options.addOption(AR_2, "Second ar", new DoubleParser(0.1));
		options.addOption(FINAL_AR_2, "Second end ar", new DoubleParser(0.05));
		
		options.addOption(N1, "First nhd", new StringParser("(roulette (random 2 2) (crossing))"));
		options.addOption(N2, "Second nhd", new StringParser("(random 2 2)"));
		
		options.addOption(E1, "Epoch length 1", new StringParser("(* 30 n)"));
		options.addOption(E2, "Epoch length 2", new StringParser("(* 10 n (- n 1))"));
		
		options.addOption(REPEATS, new IntegerParser(10));
		
		options.parse(args);
		
		double ar1, ar2, ear1, ear2, cooling1, cooling2;
		
		final Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		instance.setUsingHardConstraints(true);
		cooling1 = options.getOption(COOLING1);
		cooling2 = options.getOption(COOLING2);
		ar1= options.getOption(AR_1);
		ar2 = options.getOption(AR_2);
		ear1 = options.getOption(FINAL_AR_1);
		ear2 = options.getOption(FINAL_AR_2);
		
		final HintonRandom random = new HintonRandom();
		
		Calibrator cal = new Calibrator(instance, new INeighbourhoodConstructor() {
			@Override
			public INeighbourhood createNeighbourhood(Instance i, Solution s) {
				return NeighbourhoodSpecParser.parseSexp("(random)", i, s, random);
			}
		}, random, 50000);
		
		int el1, el2;
		
		el1 = 
			(int) ArithmeticEvaluator.evaluateSexp((String) options.getOption(E1),
					new Object[]{"n", instance.getPoints().size()}	
					);
		el2 = 
			(int) ArithmeticEvaluator.evaluateSexp((String) options.getOption(E2),
					new Object[]{"n", instance.getPoints().size()}	
					);
		
		TwoPhaseAnnealer stub = new TwoPhaseAnnealer();
		final String ns1 = options.getOption(N1);
		final String ns2 = options.getOption(N2);
		Phase p1 = stub.new Phase(new INeighbourhoodConstructor() {
			
			@Override
			public INeighbourhood createNeighbourhood(Instance i, Solution s) {
				return NeighbourhoodSpecParser.parseSexp(ns1, i, s, random);
			}
		},
				cal.calculateTemperature((float)ar1, 0.01f),
				cal.calculateTemperature((float)ear1, 0.01f),
				(float) cooling1,
				el1);
		Phase p2 = stub.new Phase(new INeighbourhoodConstructor() {
			
			@Override
			public INeighbourhood createNeighbourhood(Instance i, Solution s) {
				return NeighbourhoodSpecParser.parseSexp(ns2, i, s, random);
			}
		},
				cal.calculateTemperature((float)ar2, 0.01f),
				cal.calculateTemperature((float)ear2, 0.01f),
				(float) cooling2,
				el2);
		Timer t = Timer.getInstance();
		
		int repeats = options.getOption(REPEATS);
		double[] times = new double[repeats];
		double[] values= new double[repeats];
		for (int i = 0; i<repeats; i++) {
			t.reset();
			stub = new TwoPhaseAnnealer(instance, random, p1, p2);
			stub.run();
			times[i] = t.getElapsedTime();
			values[i] = stub.bestFitness;
		}
		System.out.println("TPResult(options=\"" + options.toString() + "\", times="+Arrays.toString(times) + ", values=" + Arrays.toString(values));
	}
	
	@Override
	public void fitnessChanged(IOptimiser source, Solution state,
			float newFitness, long ticks) {
		if (newFitness < bestFitness )
			bestFitness = newFitness;
	}
}
