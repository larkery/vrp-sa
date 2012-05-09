package h.vrp.stochasticsavings;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.NothingParser;
import h.options.Options;
import h.options.StringParser;
import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;
import h.util.timing.Timer;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.IFitnessListener;
import h.vrp.search.IOptimiser;
import h.vrp.solcons.ExtendedSavingsCalculator;
import h.vrp.solcons.SavingsConstructor;
import h.vrp.solcons.SavingsConstructor.IMoveSelector;
import h.vrp.solcons.SavingsConstructor.ISavingsCalculator;
import h.vrp.solcons.SavingsConstructor.SavingsMove;
import h.vrp.sources.SourceSpecParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class ExtendedSavingsOptimiser implements IOptimiser {
	Iterator<Float> edgeWeightIterator, radialWeightIterator, capWeightIterator;
	private List<Float> radialWeights;
	private List<Float> capWeights;
	float rw, ew, cw, brw, bew, bcw;
	private Instance instance;
	private float fitness;
	private IFitnessListener listener;
	private long ticks;
	private int repeats;
	private IMoveSelector selector;
	private boolean usePerturbation;
	private Random random;
	private double perturbation;
	
	public ExtendedSavingsOptimiser(Instance instance,
									int repeats,
									List<Float> edgeWeights, 
									List<Float> radialWeights, 
									List<Float> capWeights) {
		edgeWeightIterator = edgeWeights.iterator();
		radialWeightIterator = radialWeights.iterator();
		capWeightIterator = capWeights.iterator();
		this.radialWeights = radialWeights;
		this.capWeights = capWeights;
		
		this.repeats = repeats;
		
		rw = radialWeightIterator.next();
		ew = edgeWeightIterator.next();
		cw = capWeightIterator.next();
		this.instance = instance;
		fitness = Float.MAX_VALUE;
		ticks = 0;
	}
	
	public ExtendedSavingsOptimiser(Instance instance, 
			final Random random, 
			final double stochasticity, 
			int repeats,
			List<Float> edgeWeights, List<Float> radialWeights, List<Float> capWeights) {

		this(instance, random, stochasticity, false, repeats, edgeWeights, radialWeights, capWeights);
	}
	
	public ExtendedSavingsOptimiser(Instance instance, final Random random, final double stochasticity,
		boolean perturb, int repeats, List<Float> edgeWeights, List<Float> radialWeights, List<Float> capWeights) {
		this(instance, repeats, edgeWeights, radialWeights, capWeights);
		this.usePerturbation = perturb;
		if (perturb) {
			this.selector = SavingsConstructor.normalMoveSelector;
			this.random = random;
			this.perturbation = stochasticity;
		} else {
			this.selector = stochasticity == 1 ? SavingsConstructor.normalMoveSelector : 
				getRandomisedSelector(random, stochasticity);
		}
	}
	
	public static IMoveSelector getRandomisedSelector(final Random random, final double stochasticity) {
		return new IMoveSelector() {
			ArrayList<SavingsMove> buffer = new ArrayList<SavingsMove>();
			@Override
			public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue) {
				SavingsMove m = null;
			
				while (true) {
					m = currentQueue.poll();
					if (m == null) {
						if (buffer.isEmpty()) return m;
						else {
							m = buffer.get(buffer.size() - 1);
							currentQueue.addAll(buffer.subList(0, buffer.size()-1));
							break;
						}
					}
					if (!m.verify()) continue;
					if (random.nextDouble() > stochasticity) {
						buffer.add(m);
					} else {
						currentQueue.addAll(buffer);
						break;
					}
				}
				
				buffer.clear();
				
				return m;
			}
		};
	}
	
	public ExtendedSavingsOptimiser(Instance instance, final Random random, final int window,
			int repeats,List<Float> edgeWeights, List<Float> radialWeights, List<Float> capWeights) {
		this(instance, repeats, edgeWeights, radialWeights, capWeights);
		
		this.selector = window == 1 ? SavingsConstructor.normalMoveSelector :
			new IMoveSelector() {
				SavingsMove[] buffer = new SavingsMove[window];
				@Override
				public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue) {
					final int pick = random.nextInt(Math.min(window, currentQueue.size()));
					
					for (int i = 0; i<pick; i++) {
						buffer[i] = currentQueue.poll();
					}
					
					SavingsMove m = currentQueue.poll();
					
					for (int i = 0; i<pick; i++) {
						currentQueue.add(buffer[i]);
					}
					
					return m;
				}
			};
	}
	
	
	
	@Override
	public void setFitnessListener(IFitnessListener trace) {
		this.listener = trace;
	}

	@Override
	public boolean step() {
		SavingsConstructor sc;
		
		ISavingsCalculator calculator;
		
		if (ew == 1 && rw == 0 && cw == 0) {
			calculator = SavingsConstructor.normalSavingsCalculator;
		} else {
			calculator = new ExtendedSavingsCalculator(ew, rw, cw);
		}
		
		if (usePerturbation) {
			final ISavingsCalculator shim = calculator;
			final Random random = this.random;
			final double perturbation = this.perturbation * 2;
			
			calculator = getRandomisedCalculator(shim, random, perturbation);
		}
		
		sc = new SavingsConstructor(selector, calculator);
		
		for (int i = 0; i<repeats; i++) {
			Solution sol = sc.createSolution(instance);
			DeltaEvaluator de = new DeltaEvaluator(instance, sol);
			if (de.cost() < fitness) {
				ticks++;
				fitness = de.cost();
				brw = rw;
				bew = ew;
				bcw = cw;
				if (listener != null) {
					listener.fitnessChanged(this, sol, fitness, ticks);
				}
			}
		}
		
		return stepValues();
	}

	private boolean stepValues() {
		if (capWeightIterator.hasNext()) {
			cw = capWeightIterator.next();
			return true;
		} else {
			capWeightIterator = capWeights.iterator();
			cw = capWeightIterator.next();
		}
		if (radialWeightIterator.hasNext()) {
			rw = radialWeightIterator.next();
			return true;
		} else {
			radialWeightIterator = radialWeights.iterator();
			rw = radialWeightIterator.next();
		}
		if (edgeWeightIterator.hasNext()) {
			ew = edgeWeightIterator.next();
			return true;
		} else {
			return false;
		}
	}

	static final String O_SOURCE = "source";
	static final String O_CAP_WEIGHTS = "capacityWeights";
	static final String O_RAD_WEIGHTS = "radialWeights";
	static final String O_EDGE_WEIGHTS = "edgeWeights";
	static final String O_SEED = "seed";
	private static final String O_STOCHASTICITY = "stochasticity";
	private static final String O_REPEATS = "repeats";
	private static final String O_MODE_HACK = "mode";
	private static final String O_WINDOW_SIZE = "window";
	private static final String O_PERTURBATION = "usePerturbation";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Options options = new Options("--");
		options.addOption(O_SOURCE, "Source to parse (in normal format)",
				new StringParser("(file something)"));
		options.addOption(O_CAP_WEIGHTS, "Capacity weights (FFD sort per Altiman), (list a b c) or (range low high step)",
				new StringParser("(range 0 2 0.1)"));
		options.addOption(O_RAD_WEIGHTS, "Radial weights, (list a b c) or (range low high step)",
				new StringParser("(range 0.1 2 0.1)"));
		options.addOption(O_EDGE_WEIGHTS, "Edge weights, (list a b c) or (range low high step)",
				new StringParser("(range 0 2 0.1)"));
		options.addOption(O_SEED, "Random seed", new IntegerParser());
		options.addOption(O_REPEATS, "Repeats for stochastic runs", new IntegerParser(1));
		options.addOption(O_STOCHASTICITY, "Stochasticity parameter", new DoubleParser(1));
		
		options.addOption(O_MODE_HACK, "Mode shorthand; cw does plain savings, altiman does altiman's setup, h does mine",
				new StringParser());
		
		options.addOption(O_WINDOW_SIZE, "Window size; causes window mode to happen", new IntegerParser());
		
		options.addOption(O_PERTURBATION, "Use savings perturbation mode", new NothingParser());
		
		try {
			options.parse(args);
			
			Instance instance = SourceSpecParser.parseSexp((String)options.getOption(O_SOURCE)).createInstance();
			
			ISexpEvaluator listEvaluator = new ISexpEvaluator() {
				Float floatify(Object o) {
					if (o instanceof Double) {
						Double d = (Double) o;
						return d.floatValue();
					} else if (o instanceof Integer) {
						Integer i = (Integer) o;
						return i.floatValue();
					}
					return null;
				}
				
				Double doublify(Object o) {
					if (o instanceof Double) {
						Double d = (Double) o;
						return d;
					} else if (o instanceof Integer) {
						Integer i = (Integer) o;
						return i.doubleValue();
					}
					return null;
				}
				
				@Override
				public Object evaluateList(List<Object> results) {
					String h = (String) results.get(0);
					List<Float> values = new ArrayList<Float>();
					if (h.equals("list")) {
						for (Object o : results.subList(1, results.size())) {
							values.add(floatify(o));
						}
					} else if (h.equals("range")) {
						double min, max, step;
						min = doublify(results.get(1));
						max = doublify(results.get(2));
						step = doublify(results.get(3));
						
						while (min < max + step) {
							values.add((float) min);
							min += step;
						}
					} 
					return values;
				}
				
				@Override
				public Object evaluateAtom(Object sexp) {
					return sexp;
				}
			};

			List<Float> capWeights = (List<Float>) SexpParser.evaluateString((String) options.getOption(O_CAP_WEIGHTS), listEvaluator);
			List<Float> radWeights = (List<Float>) SexpParser.evaluateString((String) options.getOption(O_RAD_WEIGHTS), listEvaluator);
			List<Float> edgeWeights = (List<Float>) SexpParser.evaluateString((String) options.getOption(O_EDGE_WEIGHTS), listEvaluator);
			
			Random r = options.hasOption(O_SEED) ? new Random((Integer)options.getOption(O_SEED)) : new Random();
			
			int repeats = (Integer) options.getOption(O_REPEATS);
			double stochasticity = (Double) options.getOption(O_STOCHASTICITY);
			boolean usePerturbation = false; 
			if (options.hasOption(O_MODE_HACK)) {
				String mode = (String) options.getOption(O_MODE_HACK);
				if (mode.equals("cw")) {
					capWeights.clear();
					radWeights.clear();
					edgeWeights.clear();
					capWeights.add(0f);
					radWeights.add(0f);
					edgeWeights.add(1f);
					repeats = 1;
					stochasticity = 1;
				} else if (mode.equals("altiman")) {
					//do nothing?
					stochasticity = 1;
					repeats = 1;
				} else if (mode.equals("h") || mode.equals("g")) {
					repeats = capWeights.size() * edgeWeights.size() * radWeights.size();
					capWeights.clear();
					radWeights.clear();
					edgeWeights.clear();
					capWeights.add(0f);
					radWeights.add(0f);
					edgeWeights.add(1f);
				} else if (mode.equals("h+") || mode.equals("g+")) {
					System.err.println("Finding best altiman parameters...");
					ExtendedSavingsOptimiser eso = new ExtendedSavingsOptimiser(instance, r, 1, 1, edgeWeights, radWeights, capWeights);
					
					repeats = 1;
					while (eso.step()) repeats++;

					System.err.println("incidentally, performance was " + eso.fitness);
					capWeights.clear();
					radWeights.clear();
					edgeWeights.clear();
					capWeights.add(eso.bcw);
					edgeWeights.add(eso.bew);
					radWeights.add(eso.brw);
				}
				
				if (mode.startsWith("g")) {
					usePerturbation = true;
				}
			}
			System.err.println("cap: " + capWeights);
			System.err.println("rad: " + radWeights);
			System.err.println("edge: " + edgeWeights);
			System.err.println(capWeights.size() + " x " + radWeights.size() + " x " + edgeWeights.size() + " = " + (radWeights.size() * capWeights.size() * edgeWeights.size()));
			
			Timer t = Timer.getInstance();
			t.reset();
			
			ExtendedSavingsOptimiser eso = null;
			if (options.hasOption(O_WINDOW_SIZE)) {				
				final int i = (Integer) options.getOption(O_WINDOW_SIZE);
				eso = new ExtendedSavingsOptimiser(instance, r, i, repeats, edgeWeights, radWeights, capWeights);
			} else {
				if (usePerturbation || (Boolean) options.getOption(O_PERTURBATION)) {
					eso = new ExtendedSavingsOptimiser(instance, r, stochasticity, true, repeats, edgeWeights, radWeights, capWeights);
				} else {
					eso = new ExtendedSavingsOptimiser(instance, r, stochasticity, repeats, edgeWeights, radWeights, capWeights);
				}
			}
			
			int combinations = 1;
			while (eso.step()) {
				combinations++;
			}
			final double time = t.getElapsedTime();
			System.out.println("SavingsResult(");
			System.out.println(indent("parameters=\n" + indent(options.toString()) + ","));
			System.out.println(indent("best_value=" + eso.fitness) + ", ");
			System.out.println(indent("best_parameters={'rw':"+ eso.brw + ", 'ew':" + eso.bew + ", 'cw':" + eso.bcw) + "}, ");
			System.out.println(indent("tests=" + combinations * repeats) + ", ");
			System.out.println(indent("combinations=" + combinations) + ", ");
			System.out.println(indent("runtime=" + time) + ", ");
			System.out.println(indent("test_time=" + (time / (repeats * combinations))) + ")");
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	
	private static String indent(String str) {
		return "    " + str.replace("\n", "\n    ");
	}

	public static ISavingsCalculator getRandomisedCalculator(
			final ISavingsCalculator shim, final Random random,
			final double perturbation) {
		return new ISavingsCalculator() {
			@Override
			public float calculateSavings(Instance instance, SavingsMove move) {
				return (float) (shim.calculateSavings(instance, move) *
				  	(1 + (random.nextDouble() - 0.5) * perturbation));
				
			}
		};
	}

	@Override
	public boolean finished() {
		return false;
	}
}
