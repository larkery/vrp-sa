package h.vrp.model.evaluation.parts;

import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.evaluation.ICalculator;
import h.vrp.model.evaluation.ICost;
import h.vrp.model.evaluation.ITest;
import h.vrp.model.evaluation.DeltaEvaluator.IEdgeCalculator;

/**
 * Calculator for edge costs (i.e. distance and distance constraints)
 * @author hinton
 */
public class EdgeCalculator implements ICalculator, IEdgeCalculator {
	float currentCost, testCost;
	Instance instance;
	final EdgeCalculator me = this;
	private Route route;
	
	public EdgeCalculator(Instance instance, Route initial) {
		this.instance = instance;
		this.route = initial;//haaack
		currentCost = testCost = instance.sumEdges(initial);
	}

	@Override
	public void accept() {
		currentCost = testCost;
		route.setCost(currentCost);
	}

	@Override
	public void reject() {
		testCost = currentCost;
	}

	@Override
	public void test(Route r) {
		testCost = instance.sumEdges(r);
		r.setCost(testCost);
	}
	
	public ICost getCost() {
		return new ICost() {
			@Override
			public float cost() {
				return me.testCost;
			}
			public String toString() {
				return "edge cost: " + cost();
			}
		};
	}
	
	public ITest getTest(final float threshold) {
		return new ITest() {
			@Override
			public boolean isFeasible() {
				return me.testCost <= threshold;
			}
			public String toString() {
				return "distance " + (isFeasible() ? "feasible" : "infeasible") + " " + me.testCost + " <? " + threshold;
			}
			@Override
			public String getName() {
				return "length";
			}
			@Override
			public float getSlackness() {
				return (threshold - me.testCost) / threshold;
			}
		};
	}
	
	public ICost getPenalty(final float threshold, final float multiplier) {
		return new ICost() {
			@Override
			public float cost() {
				if (me.testCost <= threshold) return 0;
				else return (me.testCost - threshold) * multiplier;
			}
			
			public String toString() {
				return "distance penalty " + cost()  + " " + me.testCost + " <? " + threshold;
			}
		};
	}

	@Override
	public void addEdge(int from, int to) {
		testCost += instance.getEdgeCost(from, to);		
	}

	@Override
	public void removeEdge(int from, int to) {
		testCost -= instance.getEdgeCost(from, to);
	}
}
