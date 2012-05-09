package h.vrp.model.evaluation.parts;

import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.evaluation.ICalculator;
import h.vrp.model.evaluation.ICost;
import h.vrp.model.evaluation.ITest;
import h.vrp.model.evaluation.DeltaEvaluator.IVertexCalculator;

public class VertexCalculator implements ICalculator, IVertexCalculator {
	
	protected int currentDemand, testDemand;
	protected Instance instance;
	
	public VertexCalculator(Instance instance, Route initial) {
		this.instance = instance;
		currentDemand = testDemand = evaluate(initial);
	}
	@Override
	public void accept() {
		currentDemand = testDemand;
	}
	@Override
	public void reject() {
		testDemand = currentDemand;
	}
	@Override
	public void test(Route r) {
		testDemand = evaluate(r);
	}
	
	protected int evaluate(Route r) {
		return instance.sumDemands(r);
	}
	
	public ITest getTest(final int capacity) {
		return new ITest() {
			@Override
			public boolean isFeasible() {
				return testDemand <= capacity;
			}
			public String toString() {
				return "capacity " + (isFeasible() ? "feasible" : "infeasible") + " " + testDemand + " <? " + capacity;
			}
			@Override
			public String getName() {
				return "capacity";
			}
			@Override
			public float getSlackness() {
				return (capacity - testDemand) / (float)capacity;
			}
		};
	}
	
	public ICost getPenalty(final int capacity, final float multiplier) {
		return new ICost() {
			@Override
			public float cost() {
				if (testDemand <= capacity) return 0;
				else return (testDemand - capacity) * multiplier;
			}
			public String toString() {
				return "capacity penalty " + cost()  + " " + testDemand + " <? " + capacity;
			}
		};
	}
	@Override
	public void addVertex(int vertex) {
		testDemand += instance.getVertexDemand(vertex);
	}
	@Override
	public void removeVertex(int vertex) {
		testDemand -= instance.getVertexDemand(vertex);
	}
}
