package h.vrp.model;

import h.vrp.model.evaluation.ICalculator;
import h.vrp.model.evaluation.ICost;
import h.vrp.model.evaluation.ITest;
import h.vrp.model.evaluation.DeltaEvaluator.IEdgeCalculator;
import h.vrp.model.evaluation.DeltaEvaluator.IVertexCalculator;
import h.vrp.model.evaluation.parts.EdgeCalculator;
import h.vrp.model.evaluation.parts.KToursCalculator;
import h.vrp.model.evaluation.parts.VertexCalculator;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Fully describes an instance of a VRP, including points/distance matrices
 * demands, constraints and so on. Single depots only please kthx bye
 * @author hinton
 */
public class Instance {
	public enum ConstraintMode {
		HARD_CONSTRAINTS, SOFT_CONSTRAINTS, HARD_AND_SOFT_CONSTRAINTS, WHATEVER, NO_CONSTRAINTS
	}
	
	List<Point2D.Float> points;
	float[][] distance;
	int[] demand;
	
	int capacity;
	int vehicleCount;
	float maxLength;
	private ArrayList<Float> normalisedPoints;
	
	boolean usingHardConstraints;
	private int infeasibilityPenalty;
	private boolean isKToursInstance;
	private float meanDemand;
	private String name;
	
	boolean capacityConstraintEnabled = true;
	boolean lengthConstraintEnabled = true;
	
	public boolean isCapacityConstraintEnabled() {
		return capacityConstraintEnabled;
	}

	public void setCapacityConstraintEnabled(boolean capacityConstraintEnabled) {
		this.capacityConstraintEnabled = capacityConstraintEnabled;
	}

	public boolean isLengthConstraintEnabled() {
		return lengthConstraintEnabled;
	}

	public void setLengthConstraintEnabled(boolean lengthConstraintEnabled) {
		this.lengthConstraintEnabled = lengthConstraintEnabled;
	}

	public boolean isUsingHardConstraints() {
		return usingHardConstraints;
	}

	public void setUsingHardConstraints(boolean usingHardConstraints) {
		this.usingHardConstraints = usingHardConstraints;
	}

	public Instance(String name, List<Point2D.Float> points,
					List<Integer> demands,
					int vehicleCount,
					int capacity,
					float maxLength) {
		this.name = name;
		this.points = new ArrayList<Point2D.Float>(points);
		this.normalisedPoints = new ArrayList<Point2D.Float>(points.size());
		
		Point2D.Float depot = points.get(0);
		
		float maxDimension = 0;
		for (Point2D.Float pt : points) {
			float tx = pt.x - depot.x;
			float ty = pt.y - depot.y;
			if (tx < 0) tx =-tx;
			if (ty < 0) ty = -ty;
			if (tx > maxDimension)
				maxDimension = tx;
			if (ty > maxDimension)
				maxDimension = ty;
		}
		
		maxDimension *=2;
		
		
		for (Point2D.Float pt : points) {
			Point2D.Float transformedPoint = new Point2D.Float( 
					 (pt.x - depot.x) / maxDimension, 
					 (pt.y - depot.y) / maxDimension );
			normalisedPoints.add(transformedPoint);
		}
		
		this.demand = new int[points.size()];
		
		int i = 0;
		isKToursInstance = true;
		for (int x : demands) {
			if (i != 0 && x != 1)
				isKToursInstance = false;
			demand[i++] = x;
			meanDemand += x;
		}
		
		meanDemand /= i;
		
		if (isKToursInstance) System.err.println("Is k-tours instance, using faster mode");
		
		this.capacity = capacity;
		this.maxLength = maxLength;
		this.vehicleCount = vehicleCount;
		
		computeDistances();
	}
	
	private void computeDistances() {
		distance = new float[points.size()][points.size()];
		for (int i = 0; i<points.size(); i++) 
			for (int j = 0; j<points.size(); j++) 
				distance[i][j] = (float) points.get(i).distance(points.get(j));
	}

	public int getVehicleCount() {
		return vehicleCount;
	}

	/**
	 * Create suitable calculators, tests and costs for this instance with the given route.
	 * @param calculators see the ICalculator interface
	 * @param tests feasibility tests for this instance
	 * @param costs objective functions for this instance
	 * @param initial a route which the calculators etc will apply to
	 */
	public void prepare(List<ICalculator> calculators, List<ITest> tests,
			List<ICost> costs, Route initial) {
		prepare(calculators, tests, costs, initial, ConstraintMode.WHATEVER);
	}
	/**
	 * Create suitable calculators, tests and costs for this instance with the given route.
	 * @param calculators see the ICalculator interface
	 * @param tests feasibility tests for this instance
	 * @param costs objective functions for this instance
	 * @param initial a route which the calculators etc will apply to
	 * @param mode what kind of constraints / costs to instantiate
	 */
	public void prepare(List<ICalculator> calculators, List<ITest> tests,
			List<ICost> costs, Route initial, ConstraintMode mode) {
		EdgeCalculator ec = new EdgeCalculator(this, initial);
		VertexCalculator vc = new VertexCalculator(this, initial);
		
		calculators.add(ec); calculators.add(vc);
		
		prepareCostsAndTests(vc, ec, costs, tests, mode);
	}

	public void prepare(List<IVertexCalculator> vertexCalculators,
			List<IEdgeCalculator> edgeCalculators, List<ITest> tests, List<ICost> costs, Route route) {
		prepare(vertexCalculators, edgeCalculators, tests, costs, route, ConstraintMode.WHATEVER);
	}
	
	public void prepare(List<IVertexCalculator> vertexCalculators,
			List<IEdgeCalculator> edgeCalculators, List<ITest> tests,
			List<ICost> costs, Route route, ConstraintMode mode) {
		EdgeCalculator ec = new EdgeCalculator(this, route);
		edgeCalculators.add(ec);
//		isKToursInstance = false;
		VertexCalculator vc = 
			isKToursInstance ? new KToursCalculator(this, route) : new VertexCalculator(this, route);

		vertexCalculators.add(vc);
		prepareCostsAndTests(vc, ec, costs, tests, mode);
	}
	
	private void prepareCostsAndTests(VertexCalculator vc, EdgeCalculator ec,
			List<ICost> costs, List<ITest> tests, ConstraintMode mode) {
		if (mode == ConstraintMode.WHATEVER) {
			mode = isUsingHardConstraints() ? ConstraintMode.HARD_CONSTRAINTS : ConstraintMode.SOFT_CONSTRAINTS;
		}
		
		costs.add(ec.getCost());
		
		switch (mode) {
		case HARD_CONSTRAINTS:
			if (isLengthConstraintEnabled()) tests.add(ec.getTest(maxLength));
			if (isCapacityConstraintEnabled()) tests.add(vc.getTest(capacity));
			break;
		case HARD_AND_SOFT_CONSTRAINTS:
			if (isLengthConstraintEnabled()) tests.add(ec.getTest(maxLength));
			if (isCapacityConstraintEnabled()) tests.add(vc.getTest(capacity));
		case SOFT_CONSTRAINTS:
			if (isLengthConstraintEnabled()) costs.add(ec.getPenalty(maxLength, infeasibilityPenalty));
			if (isCapacityConstraintEnabled()) costs.add(vc.getPenalty(capacity, infeasibilityPenalty));
			break;
		case NO_CONSTRAINTS:
			//pure objective dude
		}
	}

	/**
	 * Add up the cost of the edges connecting the vertices in the given sequence, including the
	 * round-trip cost of returning to the start from the end
	 * @param sequence of vertices
	 * @return cost of traversing that sequence
	 */
	public float sumEdges(Iterable<Integer> sequence) {
		float acc = 0;
		
		Iterator<Integer> iter = sequence.iterator();
		if (!iter.hasNext()) return 0;
		int previous = iter.next();
		int current = previous;
		int first = previous;
		while (iter.hasNext()) {
			current = iter.next();
			acc += distance[previous][current];
			previous = current;
		}
		
		return acc + distance[current][first];
	}

	/**
	 * Add up the cost of the demands in the given sequence
	 * @param sequence of vertices
	 * @return sum of demands at given vertices
	 */
	public int sumDemands(Iterable<Integer> sequence) {
		int d = 0;
		for (int i : sequence) d += demand[i];
		return d;
	}

	public List<Point2D.Float> getPoints() {
		return Collections.unmodifiableList(points);
	}

	public List<Float> getNormalisedPoints() {
		return Collections.unmodifiableList(normalisedPoints);
	}

	public float getEdgeCost(int from, int to) {
		return distance[from][to];
	}

	public void setVehicleCount(int routeCount) {
		vehicleCount = routeCount;
	}

	public void setInfeasibilityPenalty(int infeasibilityPenalty) {
		this.infeasibilityPenalty = infeasibilityPenalty;
	}

	public int getVertexDemand(int vertex) {
		return demand[vertex];
	}

	public float getMeanVertexDemand() {
		return meanDemand;
	}
	
	public String toString() {
		return name;
	}

	public int getCapacity() {
		return capacity;
	}
	
	public float getMaxLength() {
		return maxLength;
	}

	public float getMeanRadius() {
		float meanRadius = 0;
		Point2D.Float origin = points.get(0);
		for (Point2D.Float pt : points) {
			meanRadius += pt.distance(origin);
		}
		meanRadius /= points.size();
		return meanRadius;
	}

	public double getMeanEdgeLength() {
		double t = 0;
		int k = 0;
		for (int i = 0; i<distance.length; i++) {
			for (int j = 0; j<i; j++) {
				k++;
				t += distance[i][j];
			}
		}
		return t / k;
	}
}
