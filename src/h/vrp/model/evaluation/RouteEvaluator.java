package h.vrp.model.evaluation;

import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Instance.ConstraintMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the cost & feasibility of a single route
 * @author hinton
 */
public class RouteEvaluator {
	List<ICalculator> calculators = new ArrayList<ICalculator>();
	List<ITest> tests = new ArrayList<ITest>();
	List<ICost> costs = new ArrayList<ICost>();
	
	public RouteEvaluator(Instance instance, Route initial) {
		//here I have to decide what objectives to employ from the instance, somehow
		//think I should set this in the instance itself.
		this (instance, initial, ConstraintMode.WHATEVER);
	}
	
	public RouteEvaluator(Instance instance, Route route, ConstraintMode mode) {
		instance.prepare(calculators, tests, costs, route, mode);
	}

	public void test(Route r) {
		for (ICalculator c : calculators) c.test(r);
	}
	
	public void accept() {
		for (ICalculator c : calculators) c.accept();
	}
	
	public void reject() {
		for (ICalculator c : calculators) c.reject();
	}

	public float cost() {
		float r = 0;
		for (ICost c : costs) r += c.cost();
		return r;
	}

	public boolean isFeasible() {
		for (ITest t : tests) if (t.isFeasible() == false) return false;
		return true;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!tests.isEmpty()) {
		sb.append(" feasibility: ");
		for (ITest test : tests) {
			sb.append(test);
			sb.append("\t");
		}
		}
		if (!costs.isEmpty()) {
		sb.append(" cost : ");
		for (ICost cost : costs) {
			sb.append(cost);
			sb.append("\t");
		}
		}
		return sb.toString();
	}
}
