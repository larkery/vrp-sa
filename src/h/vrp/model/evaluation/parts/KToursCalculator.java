package h.vrp.model.evaluation.parts;

import h.vrp.model.Instance;
import h.vrp.model.Route;

public class KToursCalculator extends VertexCalculator {
	public KToursCalculator(Instance instance, Route initial) {
		super(instance, initial);
	}

	@Override
	public void addVertex(int vertex) {
		if (vertex != 0) testDemand++;
	}

	@Override
	protected int evaluate(Route r) {
		return r.size() - 1;
	}

	@Override
	public void removeVertex(int vertex) {
		if (vertex != 0) testDemand--;
	}
}
