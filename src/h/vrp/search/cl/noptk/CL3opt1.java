package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.model.Solution;
import h.vrp.search.IMove;
import h.vrp.search.cl.Crossing;
import h.vrp.search.cl.CrossingTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CL3opt1 extends CLNoptK {

	public CL3opt1(CrossingTracker crossingList, HintonRandom random) {
		super(crossingList, random);
	}

	@Override
	public IMove sample(Solution solution) {
		return get3opt(solution, true);
	}
	
	@SuppressWarnings("unchecked")
	protected IMove get3opt(Solution solution, boolean sameRoute) {
		//find two crossings in the same route which share an edge
		List<Crossing>[] crossByRoute = new List[solution.size()];
		for (int i = 0; i<solution.size(); i++) {
			crossByRoute[i] = new ArrayList<Crossing>();
		}
		
		boolean bother = false;
		
		for (Crossing c : crossingList) {
			final int r1 = c.routeContainingE1(solution);
			final int r2 = c.routeContainingE2(solution);
			if ((sameRoute && r1 == r2) || (!sameRoute && r1 != r2)) {
				crossByRoute[r1].add(c);
				bother = bother || crossByRoute[r1].size() > 1;
			}
		}
		
		if (bother) {
			int r2 = random.nextInt(crossByRoute.length);
			while (crossByRoute[r2].size() < 2) {
				r2 = (r2 + 1) % crossByRoute.length;
			}
			final int r = r2;
			final Solution solution2 = solution;
			Comparator<Crossing> cr = 
				sameRoute ? 
				new Comparator<Crossing>() {
				@Override
				public int compare(Crossing o1, Crossing o2) {
					final int p1 = Math.min(
							o1.startE1(solution2), o1.startE2(solution2)
							//solution2.routePosition(o1.e11 != 0 ? o1.e11 : o1.e12),
							//solution2.routePosition(o1.e21 != 0 ? o1.e21 : o1.e22)
							);
					
					final int p2 = Math.min(
							o2.startE1(solution2), o2.startE2(solution2)
//							solution2.routePosition(o2.e11 != 0 ? o2.e11 : o2.e12),
//							solution2.routePosition(o2.e21 != 0 ? o2.e21 : o2.e22)
							);

					return p1-p2;
				}
			} :
				
				new Comparator<Crossing>() {

					@Override
					public int compare(Crossing o1, Crossing o2) {
						//OK, sort by position in r and then by route number
						//annoyingly we don't know which edge is in r
						final int p1 = o1.firstIndexInRoute(solution2, r);
						final int p2 = o2.firstIndexInRoute(solution2, r);
						if (p1 == p2) {
							final int ra = o1.otherRoute(solution2, r);
							final int rb = o1.otherRoute(solution2, r);
							return ra - rb;
						} else {
							return p1 - p2;
						}
					}
				
			};
			Collections.sort(crossByRoute[r], cr);

			Iterator<Crossing> iter = crossByRoute[r].iterator();
			Crossing last = iter.next();
			while (iter.hasNext()) {
				Crossing cur = iter.next();
				int es = last.sharesEdgeWith(cur);
				if (es > 0) {
					//these crossings both involve an edge in this route
					
					if (!sameRoute) {
						if (cur.otherRoute(solution, r) != last.otherRoute(solution2, r))
							continue;
					}
					
//					int a = 0 , b = 0 , c = 0;
//					int realR = r;
					int[] cuts = null;
					switch (es) {
					case 1: // edge 1 is shared
					case 3: // edge 1 of last == edge 2 of cur
						cuts = new int[] {
								cur.codeE1(solution),
								cur.codeE2(solution),
								last.codeE2(solution)
						};
						break;
					
					case 2: // edge 2 of last == edge 1 of cur
					case 4: // edge 2 of last == edge 2 of cur
						cuts = new int[] {
								cur.codeE1(solution),
								cur.codeE2(solution),
								last.codeE1(solution)
						};
						break;
					}
					
					return new RecombiningMove(solution, cuts, random);
					
//					switch (es) {
//					case 1:
//						cuts = new int[] {
//								cur.codeE1(solution2),
//								cur.codeE2(solution2),
//								last.codeE2(solution2)
//						};
//						
//						// c is e1 in both
//						c = last.startE1(solution);
//						realR = last.routeContainingE1(solution);
//						a = last.startE2(solution);
//						b = cur.startE2(solution);
//						break;
//					case 2:
//						cuts = new int[] {
//								last.codeE1(solution2),
//								last.codeE2(solution2),
//								cur.codeE1(solution2)
//						};
//						// c is e2 in both
//						c = last.startE2(solution);
//						realR = last.routeContainingE2(solution);
//						a = last.startE1(solution);
//						b = cur.startE1(solution);
//						break;
//					case 3:
//						// c is e1 in last and e2 in cur
//						cuts = new int[] {
//								last.codeE1(solution2),
//								last.codeE2(solution2),
//								cur.codeE1(solution2)
//						};
//						c = last.startE1(solution);
//						realR = last.routeContainingE1(solution);
//						a = last.startE2(solution);
//						b = last.startE1(solution);
//						break;
//					case 4:
//						//c is e2 in last and e1 in cur
//						c = last.startE2(solution);
//						realR = last.routeContainingE2(solution);
//						a = last.startE1(solution);
//						b = last.startE2(solution);
//						break;
//					}
//					
//					int t = Math.min(a, b);
//					b = Math.max(a, b);
//					a = t;
//					
//					
//					
//					if (sameRoute)
//						return new M3O1(solution.get(realR), a, b, c);
////						return new M3O1(solution, realR, a, b, c);
//					else
//						return new M3O2(solution.get(cur.otherRoute(solution, realR)),
//								solution.get(realR), a, b, c);
////						return new M3O2(solution, cur.otherRoute(solution, realR),realR, a, b, c);
				}
			}
		
		}
		return null;
	}

	@Override
	public int size(Solution solution) {
		return crossingList.size() * crossingList.size(); //kind of?
	}
}
