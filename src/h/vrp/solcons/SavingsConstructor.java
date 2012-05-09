package h.vrp.solcons;

import h.vrp.model.IRouteModification;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.IRouteModification.IEdgeIterator;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.search.IMove;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * An implementation of Clarke & Wright's savings heuristic for solution construction
 * @author hinton
 */
public class SavingsConstructor implements ISolutionConstructor {
	public interface IMoveSelector {
		/**
		 * Should select and return one move from the current queue; afterwards the queue should
		 * contain all valid savingsmoves it originally contained except the selected move
		 * @param currentQueue
		 * @return
		 */
		public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue);
	}
	
	public interface ISavingsCalculator {
		public float calculateSavings(Instance instance, SavingsMove move);
	}
	
	/**
	 * Pulls the first valid move off the queue
	 */
	public static IMoveSelector normalMoveSelector = new IMoveSelector() {
		@Override
		public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue) {
			SavingsMove m = null;
			do {
				m = currentQueue.poll();
			} while (m == null);
			return m;
		}
	};
	
	public static ISavingsCalculator normalSavingsCalculator = new ISavingsCalculator() {
		@Override
		public float calculateSavings(Instance instance, SavingsMove move) {
			final int t = move.r1.get(-1);
			final int h = move.r2.get(1);
			
			return instance.getEdgeCost(t, 0) + instance.getEdgeCost(0, h) - instance.getEdgeCost(t, h);
		}
	};

	ISavingsCalculator savingsCalculator;
	IMoveSelector moveSelector;

	public SavingsConstructor(IMoveSelector moveSelector, ISavingsCalculator savingsCalculator) {
		this.moveSelector = moveSelector;
		this.savingsCalculator = savingsCalculator;
	}
	
	public SavingsConstructor() {
		this(normalMoveSelector, normalSavingsCalculator);
	}
	
	public SavingsConstructor(IMoveSelector iMoveSelector) {
		this(iMoveSelector, normalSavingsCalculator);
	}

	public SavingsConstructor(ISavingsCalculator calculator) {
		this(normalMoveSelector, calculator);
	}
	
	public PriorityQueue<SavingsMove> createSavingsTable(Instance instance, Solution sol) {
		PriorityQueue<SavingsMove> moves = new PriorityQueue<SavingsMove>(sol.size() * sol.size() / 2);
		
		boolean printStatus = sol.size() > 300;
		if (printStatus) System.err.print("Creating savings table: ");
		//prepare cost table
		int tenpercent = sol.size() / 10;
		for (int i = 0; i<sol.size(); i++) {
			for (int j = 0; j<sol.size(); j++) {
				if (i == j) continue;
				final Route r1 = sol.get(i);
				final Route r2 = sol.get(j);
				SavingsMove sm = new SavingsMove(r1, r2);
				
				sm.setCost(savingsCalculator.calculateSavings(instance, sm));
				
				moves.add(sm);
			}
			tenpercent--;
			if (tenpercent <= 0) {
				tenpercent = sol.size() / 10;
				if (printStatus) {
					System.err.print(100 * i / sol.size() + "% ");
				}
			}
		}
		return moves;
	}
	
	
	
	protected Solution runMerge(Instance instance, PriorityQueue<SavingsMove> moves, Solution sol) {
		//now merge feasible routes having maximum savings
		boolean wasUsingHCs = instance.isUsingHardConstraints();
		instance.setUsingHardConstraints(true);
		
		DeltaEvaluator evaluator = new DeltaEvaluator(instance, sol);
		
		boolean printStatus = sol.size() > 300;
		
		if (printStatus) System.err.println();
		if (printStatus) System.err.print("Combining routes: ");
		
		int tenpercent = sol.size() / 10;
		tenpercent = sol.size() / 10;
		int routesMerged = 0;
		
		while (moves.isEmpty() == false) {
			SavingsMove m = moveSelector.selectMove(moves);
			if (m != null) {
				tenpercent--;
				routesMerged++;
				if (tenpercent <= 0) {
					tenpercent = sol.size() / 10;
					if (printStatus) System.err.print(100 * routesMerged / sol.size()+ "% ");
				}

				evaluator.test(m.getChanges());
				
				if (evaluator.isFeasible()) {
					evaluator.accept();
					sol.applyMove(m);
					//recompute savings for the surviving route;
					final Route r1 = m.r1;
					final int r2index = m.r2.index;
					
					Iterator<SavingsMove> miter = moves.iterator();
					//clear out invalid moves
					while (miter.hasNext()) {
						final SavingsMove sm = miter.next();
						
						if (sm.r2.index == r2index || sm.r1.index == r2index ||
								sm.r2.index == r1.index || sm.r1.index == r1.index) {
							if (sm.r1.index == r1.index || sm.r2.index == r2index) {
								miter.remove(); //r1 can no longer be head-joined to anything else
							} else if (sm.r1.index == r2index) {
								if (sm.r2.index == r1.index) {
									miter.remove();
								} else {
									sm.r1 = r1; //the move would have joined r2 onto another route; r2 is now the tail of r2
								}
							}
						}
					}
				} else {
					evaluator.reject();
				}
			}
		}
//		System.out.println(evaluator);
		if (printStatus) System.err.println();
		//count the number of live routes
		int i = 0;
		for (Route r : sol) {
			if (r.size() > 1) {
				i++;
			}
		}
		if (i > instance.getVehicleCount()) {
//			System.err.println("Warning: vehicle count (" + instance.getVehicleCount() + ") < route count (" + i +")");
		} else {
			i = (int)(instance.getVehicleCount()*1.5);
		}
		//hack
//		i = instance.getPoints().size()-1;
		
		Solution sol2 = new Solution(i, instance.getPoints().size());
		Iterator<Route> ir = sol2.iterator();
		for (Route r : sol) {
			if (r.size() > 1) {
				Route r2 = ir.next();
				r2.addAll(r);
			}
		}
		while (ir.hasNext()) {
			Route r2 = ir.next();
			r2.add(0);
		}
		sol2.initialize();
		
		//restore the status quo
		instance.setUsingHardConstraints(wasUsingHCs);
		
		return sol2;
	}
	
	protected Solution getBlankSolution(Instance instance) {
		Solution sol = new Solution(instance.getPoints().size()-1, instance.getPoints().size());
		
		for (int i = 1; i<instance.getPoints().size(); i++) {
			sol.get(i-1).add(0);
			sol.get(i-1).add(i);
		}
		
		sol.initialize();
		return sol;
	}
	
	public Solution createSolution(Instance instance, PriorityQueue<SavingsMove> moves) {
		Solution sol = getBlankSolution(instance);
		
		for (SavingsMove sm : moves)
			sm.resetSolution(sol);
		
		return runMerge(instance, moves, sol);
	}
	
	@Override
	public Solution createSolution(Instance instance) {
		Solution sol = getBlankSolution(instance);
		return runMerge(instance, createSavingsTable(instance, sol), sol);
	}
	

	
	protected static IEdgeIterator sei(final int a, final int b) {
		return new IEdgeIterator() {
			boolean hasNext = true;
			@Override
			public int to() {
				return b;
			}
			
			@Override
			public void next() {
				hasNext = false;
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}
			
			@Override
			public int from() {
				return a;
			}
		};
	}
	
	/**
	 * Represents a savings move
	 * @author hinton
	 */
	
	public final class SavingsMove implements IMove, Comparable<SavingsMove>, Cloneable {
		public Route r1;
		Route r2;
		private float cost;

		public SavingsMove(Route r1, Route r2) {
			this.r1 = r1;
			this.r2 = r2;
		}
		
		public void resetSolution(Solution newSolution) {
			r1 = newSolution.get(r1.index);
			r2 = newSolution.get(r2.index);
		}
		
		@Override
		public final void apply() {
			r1.addAll(r2.subList(1, r2.size()));
			r2.subList(1, r2.size()).clear();
		}
		
		public final void setCost(float cost) {
			this.cost = cost;
		}

		@Override
		public final boolean verify() {
			return true;
//			return r1.size() == sizer1 && r2.size() == sizer2;
		}

		
		public final String toString() {
			return "S(" + r1.index + ", " + r2.index + ", " + cost + ")";
		}

		@Override
		public final IRouteModification[] getChanges() {
			IRouteModification[] result = new IRouteModification[2];
			result[0] = new IRouteModification() {
				
				@Override
				public Route unmodifiedRoute() {
					return r1;
				}
				
				@Override
				public IVertexIterator removedVertices() {
					return IRouteModification.emptyVertexIterator;
				}
				
				@Override
				public IEdgeIterator removedEdges() {
					return sei(r1.get(-1), 0);
				}
				
				@Override
				public IEdgeIterator deletedEdges() {
					if (r1.size() > 2) return removedEdges();
					else return emptyEdgeIterator;
				}
				
				@Override
				public IEdgeIterator createdEdges() {
					return sei(r1.at(-1), r2.at(1));
				}
				
				@Override
				public IVertexIterator addedVertices() {
					return new IVertexIterator() {
						Iterator<Integer> it = r2.iterator();
						{
							it.next();
						}
						@Override
						public boolean hasNext() {
							return it.hasNext();
						}

						@Override
						public int next() {
							return it.next();
						}
					};
				}
				
				@Override
				public IEdgeIterator addedEdges() {
					return new IEdgeIterator() {
						Iterator<Integer> it = r2.iterator();
						int from, to;
						private boolean hasNext;
						{
							it.next();
							from = to = r1.get(-1);
							hasNext = true;
						}
						@Override
						public int to() {
							return to;
						}
						
						@Override
						public void next() {
							from = to;
							if (it.hasNext()) {
								to = it.next();
							} else {
								to = 0;
								hasNext = false;
							}
						}
						
						@Override
						public boolean hasNext() {
							return hasNext;
						}
						
						@Override
						public int from() {
							return from;
						}
					};
				}
			};
			
			result[1] = new IRouteModification() {
				
				@Override
				public Route unmodifiedRoute() {
					return r2;
				}
				
				@Override
				public IVertexIterator removedVertices() {
					return new IVertexIterator() {
						Iterator<Integer> it = r2.iterator();
						{
							it.next();
						}
						@Override
						public int next() {
							return it.next();
						}
						
						@Override
						public boolean hasNext() {
							return it.hasNext();
						}
					};
				}
				
				@Override
				public IEdgeIterator removedEdges() {
					return new IEdgeIterator() {
						boolean hasNext;
						Iterator<Integer> it = r2.iterator();
						int from, to;
						{
							to = it.next();
							hasNext = it.hasNext();
						}
						@Override
						public int to() {
							return to;
						}
						
						@Override
						public void next() {
							from = to;
							if (it.hasNext()) {
								to = it.next();
							} else {
								to = 0;
								hasNext = false;
							}
						}
						
						@Override
						public boolean hasNext() {
							return hasNext;
						}
						
						@Override
						public int from() {
							return from;
						}
					};
				}
				
				@Override
				public IEdgeIterator deletedEdges() {
					if (r2.size() > 2) return sei(0, r2.at(1));
					else return emptyEdgeIterator;
				}
				
				@Override
				public IEdgeIterator createdEdges() {
					return IRouteModification.emptyEdgeIterator;
				}
				
				@Override
				public IVertexIterator addedVertices() {
					return IRouteModification.emptyVertexIterator;
				}
				
				@Override
				public IEdgeIterator addedEdges() {
					return IRouteModification.emptyEdgeIterator;
				}
			};
			
			return result;
		}

		@Override
		public int compareTo(SavingsMove o) {
			if (o.cost < cost) {
				return -1;
			} else if (o.cost == cost) {
				return 0;
			} else {
				return 1;
			}
		}
	}

	public PriorityQueue<SavingsMove> createSavingsTable(Instance instance) {
		return createSavingsTable(instance, getBlankSolution(instance));
	}
}