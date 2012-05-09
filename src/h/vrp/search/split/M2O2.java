package h.vrp.search.split;

import h.vrp.model.IRouteModification;
import h.vrp.model.Route;
import h.vrp.search.IMove;

import java.util.List;

/**
 * A 2-opt-2 move; this cuts two routes and stitches them the other way
 * @author hinton
 *
 */
public class M2O2 implements IMove {
	Route route1, route2;
	private int break1;
	private int break2;

	public M2O2(Route route1, Route route2, int a, int b) {
		this.route1 = route1;
		this.route2 = route2;
		this.break1 = a;
		this.break2 = b;
	}

	@Override
	public void apply() {
		// we swap the second halves over, so this is actually like 4opt2 but different
		int t;
		if ((route1.size() - break1) > (route2.size() - break2)) {
			Route r = route1;
			route1 = route2;
			route2 = r;
			t = break1;
			break1 = break2;
			break2 = t;
		}
		//keep a in r1 and b in r2
		int j = break2+1;
		for (int i = break1+1; i<route1.size(); i++, j++) {
			t = route1.get(i);
			route1.set(i, route2.get(j));
			route2.set(j, t);
		}
		
		//now we have to shove the end of r2 onto r1
		List<Integer> tail = route2.subList(j, route2.size());
		route1.addAll(tail);
		//and clear the end of r2
		tail.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + route1.index + ") ");
		sb.append("[");
		for (int i = 0; i<route1.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(route1.get(i));
			if (i == break1)
				sb.append("/");
		}
		sb.append("] x ("+ route2.index + ") [");
		for (int i = 0; i<route2.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(route2.get(i));
			if (i == break2)
				sb.append("/");
		}
		sb.append("]");
		return "2-opt-2: " + sb.toString(); 
	}

	@Override
	public boolean verify() {
		return break1 >= 0 && break2 >= 0 && break1 < route1.size() && break2 < route2.size();
	}

	@Override
	public IRouteModification[] getChanges() {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public void syncSolution() {
//		solution.deleteEdge(route1.get(break1), route1.get(break1+1));
//		solution.deleteEdge(route2.get(break2), route2.get(break2+1));
//		solution.addEdge(route1.get(break1), route2.get(break2+1));
//		solution.addEdge(route2.get(break2), route1.get(break1+1));
//		
//		for (int i = break1+1; i<route1.size();i++) {
//			solution.setRouteAndPosition(route1.get(i), route1.index, i);
//		}
//		for (int i = break2+1; i<route2.size(); i++){
//			solution.setRouteAndPosition(route2.get(i), R2, i);
//		}
//	}
	
	
}
