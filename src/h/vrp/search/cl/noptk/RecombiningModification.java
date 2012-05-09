package h.vrp.search.cl.noptk;

import h.vrp.model.IRouteModification;
import h.vrp.model.Route;

public class RecombiningModification implements IRouteModification {
	Route myRoute;
	RecombiningMove move;
	
	int chunkCount;
	int[][] chunks;
	final int changedRouteIndex;
	final int routeIndex;
	final int maxIndex;
	
	int cutCount;
	int[] cuts;
	
	int[] destinations;
	
	public RecombiningModification(RecombiningMove move, int changedRouteIndex) {
		this.routeIndex = move.routes[changedRouteIndex];
		this.changedRouteIndex = changedRouteIndex;
		
		myRoute = move.solution.get(routeIndex);
		this.move = move;
		chunkCount = move.assignedChunks[changedRouteIndex];
		chunks = move.chunks[changedRouteIndex];
		
		cutCount = move.ccs[routeIndex];
		maxIndex = myRoute.size()-1;
		
		boolean gotHeadCut = move.cuts[routeIndex][0] == 0;
		boolean gotTailCut = move.cuts[routeIndex][cutCount] == move.cuts[routeIndex][cutCount-1];
		
		if (!gotHeadCut) cutCount++;
		if (!gotTailCut) cutCount++;
		
		destinations = new int[cutCount];
		cuts = new int[cutCount];
		
		if (!gotHeadCut) {
			//set head cut
			cuts[0] = 0;
			destinations[0] = move.destinations[changedRouteIndex][0];
			//copy the rest of the cuts
			for (int i = 0; i<move.ccs[routeIndex]; i++) {
				cuts[i+1] = move.cuts[routeIndex][i];
				destinations[i+1] = move.destinations[changedRouteIndex][i+1];
			}
		} else {
			//copy the cuts normally
			for (int i = 0; i<move.ccs[routeIndex]; i++) {
				cuts[i] = move.cuts[routeIndex][i];
				destinations[i] = move.destinations[changedRouteIndex][i+1];
			}
		}
		
		if (!gotTailCut) {
			//add a tail cut
			cuts[cutCount-1] = maxIndex;
		}
		//set the destination for the tail correctly
		destinations[cutCount-1] = move.destinations[changedRouteIndex][move.ccs[routeIndex]];
//		if (cuts[0] == cuts[1]) {
//			int[] trueCuts = move.cuts[routeIndex];
//			int trueCC = move.ccs[routeIndex];
//			throw new ArrayIndexOutOfBoundsException();
//		}
	}
	
	@Override
	public Route unmodifiedRoute() {
		return myRoute;
	}

	@Override
	public IEdgeIterator addedEdges() {
		return new NormalEI() {
			int skipTo;
			
			int[] chunk;
			Route chunkRoute;
			int chunkIndex;
			int index, stopIndex;
			int d;
			
			{
				from = to = skipTo = 0; //every solution starts at 0
				chunkIndex = 0;
				hasNext = true;
			}
			
			@Override
			public void next() {
				if (chunk != null) {
					if (index == stopIndex || chunkRoute.index == routeIndex) {
						//advance; keep old from.
						from = chunkRoute.at(stopIndex);
						chunk = null;
						chunkIndex++;
					} else {
						index += d;
						from = to;
						to = chunkRoute.at(index);
					}
				}
				
				if (chunk == null) {
					if (chunkIndex < chunkCount) {
						chunk = chunks[chunkIndex];

						chunkRoute = move.solution.get(chunk[0]);
						if (chunk[2] == 0) {
							d = 1;
							index = chunk[1] + 1;
							stopIndex = chunk[3];
						} else {
							d = -1;
							index = chunk[3];
							stopIndex = chunk[1]+1;
						}
						to = chunkRoute.at(index);
					} else {
						to = 0;
						hasNext = false;
					}
				}
			}
		};
//		return IRouteModification.emptyEdgeIterator;
	}

	@Override
	public IEdgeIterator removedEdges() {
		return new NormalEI() {
			int index, stopIndex;
			
			int cutIndex;
			
			{
				cutIndex = -1;
				hasNext = nextCut();
			}
			
			boolean nextCut() {
				cutIndex++;
				
				if (cutIndex >= cutCount) return false;
				
				index = cuts[cutIndex];
				
				if ((cutIndex + 1 == cutCount)  //this case happens when we hit the end
						|| (destinations[cutIndex] == routeIndex)) {
					stopIndex = index; //only do one step
				} else {
					stopIndex = cuts[cutIndex+1] - 1;// run until the vertex two before the next cut (two, because that cut will be counted later)
				}
				
				return true;
			}
			
			@Override
			public void next() {
				from = myRoute.at(index);
				to = myRoute.at(index+1);
				if (index == stopIndex) {
					hasNext = nextCut();
				} else {
					index++;
				}
			}
		};
//		return IRouteModification.emptyEdgeIterator;
	}

	@Override
	public IVertexIterator addedVertices() {
		if (move.routeCount < 2) {
			return IRouteModification.emptyVertexIterator;
		}
		
		return new IVertexIterator() {
			int chunkIndex;
			Route route;
			int indexInRoute, stopIndex;
			boolean hasNext;
			int[] chunk;
			{
				chunkIndex = -1;
				hasNext = nextChunk();
			}
			
			boolean nextChunk() {
				chunkIndex++;
				if (chunkIndex >= chunkCount) return false;
				chunk = chunks[chunkIndex];
				if (chunk[0] == routeIndex || chunk[1] == chunk[3]) {
					return nextChunk();
				} else {
					route = move.solution.get(chunk[0]);
					indexInRoute = chunk[1]+1;
					stopIndex = chunk[3]+1;
					return true;
				}
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public int next() {
				int next = route.at(indexInRoute);
				indexInRoute++;
				if (indexInRoute >= stopIndex) {
					hasNext = nextChunk();
				}
				return next;
			}
		};
	}
	
	@Override
	public IVertexIterator removedVertices() {
		if (move.routeCount < 2) {
			return IRouteModification.emptyVertexIterator;
		}
		
		return new IVertexIterator() {
			int cutIndex;
			
			int index, stopIndex;
			
			boolean hasNext;
			{
				cutIndex = -1;
				hasNext = nextCut();
			}
			
			boolean nextCut() {
				cutIndex++;
				if (cutIndex >= cutCount) return false;
				if (destinations[cutIndex] == routeIndex) return nextCut();
				
				index = cuts[cutIndex] + 1;
				stopIndex = cutIndex + 1 == cutCount ? maxIndex : cuts[cutIndex+1] + 1;
				
				if (index == stopIndex) return nextCut();
				
				return true;
			}
			
			@Override
			public int next() {
				int next = myRoute.at(index);
				index++;
				if (index >= stopIndex) hasNext = nextCut();
				return next;
			}
			
			@Override
			public boolean hasNext() {
				return hasNext;
			}
		};
	}

	@Override
	public IEdgeIterator deletedEdges() {
		//all cut edges
		return new NormalEI() {
			int cut;
			
			{
				cut = -1;
				hasNext = nextCut();
			}
			
			boolean nextCut() {
				cut++;
				if (cut == cutCount) return false;
				return true;
			}
			
			@Override
			public void next() {
				from = myRoute.at(cuts[cut]);
				to = myRoute.at(cuts[cut]+1);
				hasNext = nextCut();	
			}
		};
	}
	
	
	
	@Override
	public IEdgeIterator createdEdges() {
		return new NormalEI() {
			int chunkIndex = 0;
			int endOfLastChunk = 0;
			int[] chunk;
			
			{
				hasNext = true;
//				System.err.println("Chunk count : " + chunkCount + " for " + routeIndex);
			}
			
			@Override
			public void next() {
//				if (chunkCount == 0) {
//					System.err.println("break");
//				}
				if (chunk != null) {
					chunk = null;
					chunkIndex++;
					if (chunkIndex >= chunkCount) {
						hasNext = false;
						from = endOfLastChunk;
						to = 0;
						return;
					}
				}
				if (chunk == null) {
					chunk = chunks[chunkIndex];
					
					from = endOfLastChunk;
					Route r = move.solution.get(chunk[0]);
					if (chunk[2] == 0) {
						to = r.at(chunk[1]+1);
						endOfLastChunk = r.at(chunk[3]);
					} else {
						to = r.at(chunk[3]);
						endOfLastChunk = r.at(chunk[1]+1);
					}
					
					if (chunkIndex >= chunkCount) {
						hasNext = false;
					}
				}
			}
		}; 
	}



	abstract class NormalEI implements IEdgeIterator {
		int from, to;
		boolean hasNext;
		public NormalEI() {}
		@Override public boolean hasNext() { return hasNext; }
		@Override public int from() { return from; }
		@Override public int to() { return to; }
	}
	
	
	static String str(IVertexIterator vi) {
		StringBuilder sb = new StringBuilder();
		while (vi.hasNext()) {
			sb.append(" " + vi.next());
		}
		return sb.toString();
	}
	
	static String str(IEdgeIterator ei) {
		StringBuilder sb = new StringBuilder();
		while (ei.hasNext()) {
			ei.next();
			sb.append(" (" + ei.from() + ", " + ei.to() + ")");
		}
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		
		sb.append("RecominbingModification for route " + routeIndex + ", " + myRoute + "\n");
		sb.append("Added Edges: " + str(addedEdges()) + "\n");
		sb.append("Removed Edges: " + str(removedEdges()) + "\n");
		sb.append("Added Vertices: " + str(addedVertices()) + "\n");
		sb.append("Removed Vertices: " + str(removedVertices()) + "\n");
		
		return sb.toString();
	}
}
