package h.vrp.search;

import h.defines.Defines;
import h.util.timing.Timer;
import h.vrp.model.IRouteModification;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.model.Instance.ConstraintMode;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.cl.noptk.RecombiningMove;

import java.util.Random;

public class Annealer implements IOptimiser {
	private INeighbourhood neighbourhood;
	private Solution solution;
	private Random random;
	private double T;
	private float fitness;
	private float bestFitness;
	private DeltaEvaluator evaluator;
	private float delta;
	private IFitnessListener listener;
	private long ticks;
	private Instance instance;
	private boolean wasFeasible;
	Timer clock;
	private int accepts;
	private int tests;
	private int zeros;
	private int improvements;
	private double alpha;
	private int epochLength;
	private int epochCounter = 0;

	/**
	 * Constructor which causes the annealer to set the temperature by time elapsed out of a total allowed quantity
	 * @param instance
	 * @param initialSolution
	 * @param neighbourhood
	 * @param random
	 * @param allottedTime
	 * @param annealingParameter
	 */
	public Annealer(Instance instance, Solution initialSolution, INeighbourhood neighbourhood, int epochLength, double T0, double alpha,
			Random random) {
		this.instance = instance;
		this.solution = initialSolution;
		this.neighbourhood = neighbourhood;
		this.random = random;

		this.T = (float) T0;
		this.alpha = alpha;
		this.epochLength = epochLength;
		clock = Timer.getInstance();
		clock.reset();
		setCurrentSolution(initialSolution);
	}
	
	public final boolean step() {
		boolean accepted = false;
		IMove move = neighbourhood.sample(solution);

		if (move != null) {
			ticks++;
			IRouteModification[] changes = move.getChanges();
			evaluator.test(changes);
			
			final boolean isFeasible = evaluator.isFeasible();
			
			if (isFeasible) {								//new solution is feasible
				final float newCost = evaluator.cost();		//get new solution cost
				
				
				
				
				if (!wasFeasible) {							//old solution was not feasible, so
					accept(isFeasible, newCost, move, changes);			// move to new solution no matter the cost
					accepted = true;
					System.err.println("Entered feasible region, annealing properly");
					//create a new evaluator which is more efficient
					evaluator = new DeltaEvaluator(instance, solution);
//					clock.reset();
				} else {
					if (acceptable(newCost)) {				//old solution was also feasible so check acceptable
						accepted = true;
						accept(isFeasible, newCost, move, changes);		//keep
					} else {
						accepted = false;
						reject();							//bad luck, kill
					}
				}
				
//				if (accepted) {
//					DeltaEvaluator check = new DeltaEvaluator(instance, solution);
//					float actualCost = check.cost();
//					if (actualCost != newCost) {
//						float error = newCost - actualCost;
//						if (Math.abs(error) > 0.05f) 
//						System.err.println("Delta was wrong; predicted cost: " + newCost + ", real cost: " + actualCost+", error:" + error);
//					}
//				}
				
			} else {
				if (!wasFeasible) {							//neither solution is feasible, so descend until feasible
					final float newCost = evaluator.cost();
					
					if (newCost < fitness) {
						accept(isFeasible, newCost, move, changes);		//moved to a better infeasible point
						accepted = true;
					} else {
						reject();							//nevermind, kill it
					}
				} else {
					reject();								//we were feasible before, and now we're not. kill.
				}
			}
			
			if (wasFeasible) {
				epochCounter++;
				if (epochCounter >= epochLength) {
					epochCounter = 0;
					T = T * alpha;
				
					evaluator = new DeltaEvaluator(instance, solution);
					
					final float c = evaluator.cost();
					if (Math.abs(c - fitness) > 0.01) {
						System.err.println("Accumulated error : " + (c-fitness) + ", resetting");
						fitness = c;
					}
					
					float ar = accepts / (float) tests;
					float zr = zeros / (float) tests;
					float ir = improvements / (float) tests;
					System.err.println("Time: " + clock.getElapsedTime() + ", T: " + T+", fitness: "+fitness+", ar:" + ar+", zr:" + zr+", ir:" + ir+", moves:" + tests);
					tests = accepts = zeros = improvements = 0;
				}
			}
		}
		return accepted;
	}
	
	/**
	 * Reject a partially applied move
	 */
	private void reject() {
		evaluator.reject();
	}
	
	/**
	 * Accept the last move, with given feasibility and fitness
	 * @param newFitness
	 */
	private void accept(boolean newFeasibility, float newFitness,IMove move, IRouteModification[] changes) {
		StringBuilder sb = new StringBuilder();
		if (Defines.VALIDATE) {
			sb.append("Before Move Application:\n");
			sb.append("\t Move = " + move +"\n");
			for (IRouteModification m : changes) {sb.append("\t "  + m.toString() + "\n");}
		}
		
		solution.applyMove(move);
		evaluator.accept();
		
		if (Defines.VALIDATE) {
			DeltaEvaluator testeval = new DeltaEvaluator(instance, solution);
			if (Math.abs(testeval.cost() - newFitness) > 0.01) {
				solution.verify();
				System.err.println("Fitness Discrepancy = " + (evaluator.cost() - testeval.cost()));
				System.err.println(sb.toString());
				System.err.println("After move application:");
				System.err.println("Bad move: " + (move instanceof RecombiningMove ? ((RecombiningMove) move).toLongString() : move.toString()));
				System.err.println("Evaluator differences:");
				for (IRouteModification mod : changes) {
					System.err.println(mod.unmodifiedRoute());
					System.err.println("GOOD: " + testeval.toString(mod.unmodifiedRoute().index));
					System.err.println("BAD: " + evaluator.toString(mod.unmodifiedRoute().index));
				}
				System.err.println("Oh dear");
			}
		}
		
		if (listener != null && newFitness != fitness)
			listener.fitnessChanged(this, solution, newFitness, ticks);
		
		fitness = newFitness;
		if (fitness < bestFitness)
			bestFitness = fitness;
		
		wasFeasible = newFeasibility;
	}

	private final boolean acceptable(float fitness) {
//		return true;
		tests++;
		delta = fitness - this.fitness;
		boolean rv;
		if (delta == 0) {rv = false;zeros++;}
		else if (delta < 0) {rv = true;improvements++;}
		else rv = random.nextDouble() < Math.exp(-delta / T);
		
		if (rv) accepts++;
		
		return rv;
	}

	public final float lastDelta() {
		return delta;
	}
	public final float getFitness() {
		return fitness;
	}
	
	public void setFitnessListener(IFitnessListener newListener) {
		this.listener = newListener;
		if (listener != null) listener.fitnessChanged(this, solution, fitness, ticks);
	}
	
	public void setCurrentSolution(Solution initialSolution) {
		this.solution = initialSolution;
		evaluator = new DeltaEvaluator(instance, solution);
		
		
		
		if (evaluator.isFeasible() == false) {
			//infeasible start solution!
			System.err.println("Warning: Start solution was infeasible! Partially softening constraints, for a bit");
			evaluator = new DeltaEvaluator(instance, solution, ConstraintMode.HARD_AND_SOFT_CONSTRAINTS);
		}
		ticks = 0;
		fitness = evaluator.cost();
		wasFeasible = evaluator.isFeasible();
		bestFitness = fitness;
	}

	public long getTicks() {
		return ticks;
	}

	@Override
	public boolean finished() {
		return T < 0.03;
	}

	public float getTemperature() {
		return (float)T;
	}
}
