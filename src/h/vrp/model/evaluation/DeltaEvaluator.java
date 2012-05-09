package h.vrp.model.evaluation;

import h.vrp.model.IRouteModification;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeltaEvaluator {
	public interface IVertexCalculator {
		void removeVertex(int vertex);
		void addVertex(int vertex);
		void accept();
		void reject();
	}
	
	public interface IEdgeCalculator {
		void removeEdge(int from, int to);
		void addEdge(int from, int to);
		void accept();
		void reject();
	}
	
	class DeltaRouteEvaluator {
		List<IVertexCalculator> vertexCalculators = new ArrayList<IVertexCalculator>();
		List<IEdgeCalculator> edgeCalculators = new ArrayList<IEdgeCalculator>();
		
		List<ICost> costs = new ArrayList<ICost>();
		List<ITest> tests = new ArrayList<ITest>();
		
		public DeltaRouteEvaluator(Instance instance, Route route) {
			instance.prepare(vertexCalculators, edgeCalculators, tests, costs, route);
		}
		
		public DeltaRouteEvaluator(Instance instance, Route route, Instance.ConstraintMode mode) {
			instance.prepare(vertexCalculators, edgeCalculators, tests, costs, route, mode);
		}
		
		public void test(IRouteModification modification) {
			//process vertices
			for (IVertexCalculator vc : vertexCalculators) {
				IRouteModification.IVertexIterator deleted = modification.removedVertices();
				while (deleted.hasNext()) {
					vc.removeVertex(deleted.next());
				}
				
				IRouteModification.IVertexIterator added = modification.addedVertices();
				while (added.hasNext()) {
					vc.addVertex(added.next());
				}
			}
			
			//now do edges
			for (IEdgeCalculator ec : edgeCalculators) {
				IRouteModification.IEdgeIterator deleted = modification.removedEdges();
				while (deleted.hasNext()) {
					deleted.next();
					ec.removeEdge(deleted.from(), deleted.to());
				}
				
				IRouteModification.IEdgeIterator added = modification.addedEdges();
				while (added.hasNext()) {
					added.next();
					ec.addEdge(added.from(), added.to());
				}
			}
		}
		
		public boolean isFeasible() {
			for (ITest t : tests) if (t.isFeasible() == false) return false;
			return true;
		}
		
		public float cost() {
			float c = 0;
			for (ICost cf : costs) c+=cf.cost();
			return c;
		}

		public void reject() {
			for (IEdgeCalculator ec : edgeCalculators) ec.reject();
			for (IVertexCalculator vc : vertexCalculators) vc.reject();
		}

		public void accept() {
			for (IEdgeCalculator ec : edgeCalculators) ec.accept();
			for (IVertexCalculator vc : vertexCalculators) vc.accept();
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

		public void addSlacknesses(Map<String, List<Float>> rv) {
			for (ITest x : tests) {
				if (!rv.containsKey(x.getName())) {
					List<Float> l = new ArrayList<Float>();
					l.add(x.getSlackness());
					rv.put(x.getName(), l);
				} else {
					rv.get(x.getName()).add(x.getSlackness());
				}
			}
		}
	}
	
	
	DeltaRouteEvaluator[] evaluators;
	
	public DeltaEvaluator(Instance instance, Solution initial, Instance.ConstraintMode mode) {
		evaluators = new DeltaRouteEvaluator[initial.size()];
		for (Route r : initial) {
			evaluators[r.index] = new DeltaRouteEvaluator(instance, r, mode);
		}
	}
	
	public DeltaEvaluator(Instance instance, Solution initial) {
		evaluators = new DeltaRouteEvaluator[initial.size()];
		for (Route r : initial) {
			evaluators[r.index] = new DeltaRouteEvaluator(instance, r);
		}
	}
	
	public void test(IRouteModification[] modifications) {
		for (IRouteModification modification : modifications) {
			evaluators[modification.unmodifiedRoute().index].test(modification);
		}
	}
	public boolean isFeasible() {
		for (DeltaRouteEvaluator e : evaluators) if (e.isFeasible() == false) return false;
		return true;
	}
	public float cost() {
		float c = 0;
		for (DeltaRouteEvaluator e : evaluators) c += e.cost();
		return c;
	}
	public void accept() {
		for (DeltaRouteEvaluator e : evaluators) e.accept();
	}
	public void reject() {
		for (DeltaRouteEvaluator e : evaluators) e.reject();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i<evaluators.length; i++) {
			sb.append(i);
			sb.append( " : " );
			sb.append(evaluators[i]);
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public String toString(int n) {
		return evaluators[n].toString();
	}
	
	public Map<String, List<Float>> getSlacknesses() {
		Map<String, List<Float>> rv = new HashMap<String, List<Float>>();
		
		for (DeltaRouteEvaluator de : evaluators) {
			de.addSlacknesses(rv);
		}
		
		return rv;
	}
}
