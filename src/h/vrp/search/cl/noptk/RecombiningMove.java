package h.vrp.search.cl.noptk;

import h.util.random.HintonRandom;
import h.vrp.model.IRouteModification;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.search.IMove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecombiningMove implements IMove {
	private static final boolean SPEEDUP_ENABLED = false;
	private int[] vertices;
	Solution solution;

	int[][] cuts;
	int[] ccs;
	int[] routes;

	int routeCount;
	List<Integer> headsAndTails;
	int[][][] chunks;
	int[] assignedChunks;
	int[][] destinations;
	private boolean deadMove;
	String tag = "generic";
	/**
	 * Construct a recombining move, which will delete the edges following the given vertices.
	 * A special hack: if a vertex is <=0 it is in fact the first edge in the route with that number.
	 * @param vertices
	 */
	public RecombiningMove(Solution solution, int[] vertices, HintonRandom random) {
		this.solution = solution;
		this.vertices = vertices;

		int vertexCount = 0;

		cuts = new int[solution.size()][vertices.length+2];
		ccs = new int[solution.size()];
		routes = new int[solution.size()];

		//find where all the cuts are
		Arrays.sort(vertices);
		int last = -solution.size();

		deadMove = true;

		for (int x : vertices) {
			if (x == last) continue;
			vertexCount++;
			last = x;
			final int ri = routeIndex(x);
			final int rp = routePosition(x);
			deadMove = deadMove && rp == 0;
			cuts[ri][ccs[ri]++] = rp;
		}

		if (deadMove) return;

		routeCount = 0;
		//order all the cuts where important
		int chunkCount = 0;
		for (int i = 0; i<ccs.length; i++) {
			if (ccs[i] > 0) {
				routes[routeCount] = i;
				routeCount++;
				chunkCount = chunkCount + ccs[i] - 1;
				Arrays.sort(cuts[i], 0, ccs[i]);
				cuts[i][ccs[i]] = solution.get(i).size()-1;
			}
		}

		//now we can start working out what the new thing will look like by picking flips and stuff
		//first arrange heads and tails

		chunks = new int[routeCount][chunkCount + 2][4];
		assignedChunks = new int[routeCount];
		destinations = new int[routeCount][vertices.length + 2];
		if (SPEEDUP_ENABLED) {
		switch (routeCount) {
		case 1:
			final int routeix = routes[0];
			switch (ccs[routeix]) {
			case 2:
				tag = "2opt1";
				if (chunkCount == 0) {
					deadMove = true;
					return;
				}

				final int[][] routeChunks = chunks[0];
				assignedChunks[0] = 3;
				int ix = 0;
				routeChunks[ix][0] = routeix;
				routeChunks[ix][1] = 0;
				routeChunks[ix][3] = cuts[routeix][0];
				destinations[0][0] = routeix;
				destinations[0][1] = routeix;
				destinations[0][2] = routeix;
				if (routeChunks[ix][1] != routeChunks[ix][3]) ix++;

				routeChunks[ix][0] = routeix;
				routeChunks[ix][1] = cuts[routeix][0];
				routeChunks[ix][3] = cuts[routeix][1];

				if (routeChunks[ix][1] != routeChunks[ix][3]) ix++;
				routeChunks[ix][0] = routeix;
				routeChunks[ix][1] = cuts[routeix][1];
				routeChunks[ix][3] = cuts[routeix][2];

				if (routeChunks[ix][1] != routeChunks[ix][3]) ix++;
				assignedChunks[0] = ix;


				routeChunks[0][2] = 1;
				routeChunks[1][2] = 0;
				routeChunks[2][2] = 1;

				if (ix < 2) {
					deadMove = true;
				}

				return;
			case 3:
				tag = "3opt1";
				//leave head and tail alone, and then rearrange the two cuts in the middle
				final int[][] rchunks = chunks[0];
				final int rindex = routes[0];
				final int[] dest = destinations[0];
				boolean flip1 = random.nextBoolean();

				dest[0] = rindex;
				dest[1] = rindex;
				dest[2] = rindex;
				dest[3] = rindex;

				int cc = 0;
				//assign head chunk
				rchunks[cc][0] = rindex;
				rchunks[cc][1] = 0;
				rchunks[cc][2] = 0;
				rchunks[cc][3] = cuts[rindex][0];
				if (rchunks[cc][1] != rchunks[cc][3]) cc++;

				//there ought to be two middle chunks. we should flip them?
				rchunks[cc][0] = rindex;
				rchunks[cc][1] = cuts[rindex][1];
				rchunks[cc][2] = flip1 ? 0 : 1;
				rchunks[cc][3] = cuts[rindex][2];					
				if (rchunks[cc][1] != rchunks[cc][3]) cc++;

				rchunks[cc][0] = rindex;
				rchunks[cc][1] = cuts[rindex][0];
				rchunks[cc][2] = flip1 ? 1 : 0;
				rchunks[cc][3] = cuts[rindex][1];			
				if (rchunks[cc][1] != rchunks[cc][3]) cc++;

				rchunks[cc][0] = rindex;
				rchunks[cc][1] = cuts[rindex][2];
				rchunks[cc][2] = 0;
				rchunks[cc][3] = cuts[rindex][3];			
				if (rchunks[cc][1] != rchunks[cc][3]) cc++;

				assignedChunks[0] = cc;
				return;
			}
			break;
		case 2:
			switch (ccs[routes[0]] + ccs[routes[1]]) {
			case 2:
				tag = "2opt2";
				final int r1 = routes[0];
				final int r2 = routes[1];


				int cc1, cc2;
				cc1 = 0;
				cc2 = 0;

				//assign head of route 1 to route 1
				chunks[0][cc1][0] = r1;
				chunks[0][cc1][1] = 0;
				chunks[0][cc1][2] = 0;
				chunks[0][cc1][3] = cuts[r1][0];
				if (chunks[0][cc1][1] != chunks[0][cc1][3]) cc1++;

				destinations[0][0] = r1;

				if (random.nextBoolean()) {
					//head 1 -> tail 2
					//and vice-versa

					chunks[0][cc1][0] = r2;
					chunks[0][cc1][1] = cuts[r2][0];
					chunks[0][cc1][2] = 0;
					chunks[0][cc1][3] = cuts[r2][1];
					if (chunks[0][cc1][1] != chunks[0][cc1][3]) cc1++;

					chunks[1][cc2][0] = r2;
					chunks[1][cc2][1] = 0;
					chunks[1][cc2][2] = 0;
					chunks[1][cc2][3] = cuts[r2][0];
					if (chunks[1][cc2][1] != chunks[1][cc2][3]) cc2++;

					chunks[1][cc2][0] = r1;
					chunks[1][cc2][1] = cuts[r1][0];
					chunks[1][cc2][2] = 0;
					chunks[1][cc2][3] = cuts[r1][1];
					if (chunks[1][cc2][1] != chunks[1][cc2][3]) cc2++;

					destinations[0][1] = r2; //tail of r1 goes to r2
					destinations[1][0] = r2; //head of r2 goes to r2
					destinations[1][1] = r1; //tail of r2 goes to r1
				} else {
					//head 1 -> head 2 and v.v.
					//tail of 1 is reversed head of 2
					chunks[0][cc1][0] = r2;
					chunks[0][cc1][1] = 0;
					chunks[0][cc1][2] = 1;
					chunks[0][cc1][3] = cuts[r2][0];
					if (chunks[0][cc1][1] != chunks[0][cc1][3]) cc1++;

					destinations[1][0] = r1; //route 2's head is to go in r1

					//head of 2 is reversed tail of 1
					chunks[1][cc2][0] = r1;
					chunks[1][cc2][1] = cuts[r1][0];
					chunks[1][cc2][2] = 1;
					chunks[1][cc2][3] = cuts[r1][1];
					if (chunks[1][cc2][1] != chunks[1][cc2][3]) cc2++;
					destinations[0][1] = r2;

					chunks[1][cc2][0] = r2;
					chunks[1][cc2][1] = cuts[r2][0];
					chunks[1][cc2][2] = 0;
					chunks[1][cc2][3] = cuts[r2][1];
					if (chunks[1][cc2][1] != chunks[1][cc2][3]) cc2++;
					destinations[1][1] = r2;
				}
				assignedChunks[0] = cc1;
				assignedChunks[1] = cc2;
				if (cc1 < 2 && cc2 < 2) {
					deadMove = true;
				}
				return;
			case 3: 
				tag = "3opt2";
				final int r1_ = ccs[routes[0]] > 1 ? routes[0] : routes[1];
				final int r2_ = r1_ == routes[0] ? routes[1] : routes[0];

				final int rc1 = r1_ == routes[0] ? 0 : 1;
				final int rc2 = r2_ == routes[1] ? 1 : 0;

				final int[][] r1chunks = chunks[rc1];
				final int[][] r2chunks = chunks[rc2];

				final int[] d1 = destinations[rc1];
				final int[] d2 = destinations[rc2];
				//r1 is source route, r2 is dest route.
				//copy patch from r1 into r2, optionally flipping it
				int cc = 0;
				//excise chunk from r1
				d1[0] = r1_;
				r1chunks[cc][0] = r1_;
				r1chunks[cc][1] = 0;
				r1chunks[cc][2] = 0;
				r1chunks[cc][3] = cuts[r1_][0];
				if (r1chunks[cc][3] != r1chunks[cc][1]) cc++;

				r1chunks[cc][0] = r1_;
				r1chunks[cc][1] = cuts[r1_][1];
				r1chunks[cc][2] = 0;
				r1chunks[cc][3] = cuts[r1_][2];

				if (r1chunks[cc][3] != r1chunks[cc][1]) cc++;
				assignedChunks[rc1] = cc;
				d1[2] = r1_;

				//insert into r2
				cc=0;
				r2chunks[cc][0] = r2_;
				r2chunks[cc][1] = 0;
				r2chunks[cc][2] = 0;
				r2chunks[cc][3] = cuts[r2_][0];
				if (r2chunks[cc][3] != r2chunks[cc][1]) cc++;
				d2[0] = r2_;

				r2chunks[cc][0] = r1_;
				r2chunks[cc][1] = cuts[r1_][0];
				r2chunks[cc][2] = random.nextBoolean() ? 1 : 0;//flip
				r2chunks[cc][3] = cuts[r1_][1];
				if (r2chunks[cc][3] != r2chunks[cc][1]) cc++;
				d1[1] = r2_;

				r2chunks[cc][0] = r2_;
				r2chunks[cc][1] = cuts[r2_][0];
				r2chunks[cc][2] = 0;
				r2chunks[cc][3] = cuts[r2_][1];
				if (r2chunks[cc][3] != r2chunks[cc][1]) cc++;
				d2[1] = r2_;

				assignedChunks[rc2] = cc;

				return;

			case 4:
				tag = "4opt2";
				//4opt2
				//keep heads, exchange middles,reverse if we fancy
				final int ra = routes[0];
				final int rb = routes[1];

				if (ccs[ra] == 2 && ccs[rb] == 2) {

					final int[][] chunksa = chunks[0];
					final int[][] chunksb = chunks[1];
					final int[] da = destinations[0];
					final int[] db = destinations[1];
					cc = 0;

					chunksa[cc][0] = ra;
					chunksa[cc][1] = 0;
					chunksa[cc][2] = 0;
					chunksa[cc][3] = cuts[ra][0];
					if (chunksa[cc][1] != chunksa[cc][3]) cc++;
					chunksa[cc][0] = rb;
					chunksa[cc][1] = cuts[rb][0];
					chunksa[cc][2] = random.nextBoolean() ? 1 : 0;
					chunksa[cc][3] = cuts[rb][1];
					if (chunksa[cc][1] != chunksa[cc][3]) cc++;
					chunksa[cc][0] = ra;
					chunksa[cc][1] = cuts[ra][1];
					chunksa[cc][2] = 0;
					chunksa[cc][3] = cuts[ra][2];
					if (chunksa[cc][1] != chunksa[cc][3]) cc++;
					da[0] = ra;
					da[1] = rb;
					da[2] = ra;

					assignedChunks[0] = cc;
					cc = 0;

					chunksb[cc][0] = rb;
					chunksb[cc][1] = 0;
					chunksb[cc][2] = 0;
					chunksb[cc][3] = cuts[rb][0];
					if (chunksb[cc][1] != chunksb[cc][3]) cc++;
					chunksb[cc][0] = ra;
					chunksb[cc][1] = cuts[ra][0];
					chunksb[cc][2] = random.nextBoolean() ? 1 : 0;
					chunksb[cc][3] = cuts[ra][1];
					if (chunksb[cc][1] != chunksb[cc][3]) cc++;
					chunksb[cc][0] = rb;
					chunksb[cc][1] = cuts[rb][1];
					chunksb[cc][2] = 0;
					chunksb[cc][3] = cuts[rb][2];
					if (chunksb[cc][1] != chunksb[cc][3]) cc++;
					db[0] = rb;
					db[1] = ra;
					db[2] = rb;

					assignedChunks[1] = cc;
					return;
				}
			}
		}
		}

		headsAndTails = new ArrayList<Integer>(routeCount * 2);

		for (int i = 0; i<routeCount*2; i++) headsAndTails.add(i);
		Collections.shuffle(headsAndTails, random);

		for (int i = 0; i<routeCount; i++) {
			int head = headsAndTails.get(i);
			int[] ci = chunks[i][assignedChunks[i]++];
			if (head >= routeCount) {
				ci[0] = routes[head-routeCount];
				ci[1] = cuts[ci[0]][ccs[ci[0]]-1];
				ci[2] = 1;
				ci[3] = solution.get(ci[0]).size()-1;
				head -= routeCount;
				destinations[head][ccs[routes[head]]] = routes[i];
			} else {
				ci[0] = routes[head];
				ci[1] = 0;
				ci[2] = 0;
				ci[3] = cuts[ci[0]][0];
				destinations[head][0] = routes[i];
			}
			if (ci[1] == ci[3]) assignedChunks[i]--;
		}

		for (int i = 0; i<routeCount; i++) {
			final int srcRoute = routes[i];
			//				final Route srcRouteObj = solution.get(routes[i]);

			for (int j = 1; j<ccs[srcRoute]; j++) {
				int destRoute = random.nextInt(routeCount);
				if (routeCount > 1 && routes[destRoute] == srcRoute) {
					destRoute = ( destRoute + 1  )% routeCount;
				}
				//avoid the identity move!
				final boolean flip = ((routeCount == 1 && ccs[srcRoute] == 2) || random.nextBoolean());

				int[] ci = chunks[destRoute][assignedChunks[destRoute]++];
				ci[0] = srcRoute;
				ci[1] = cuts[srcRoute][j-1];
				ci[2] = flip ? 1 : 0;
				ci[3] = cuts[srcRoute][j];

				if (ci[1] == ci[3]) assignedChunks[destRoute]--;

				destinations[i][j] = routes[destRoute];
			}
		}

		for (int i = 0; i<routeCount; i++) {
			int tail = headsAndTails.get(i + routeCount);
			int[] ci = chunks[i][assignedChunks[i]++];
			if (tail >= routeCount) {
				ci[0] = routes[tail-routeCount];
				ci[1] = cuts[ci[0]][ccs[ci[0]]-1];
				ci[2] = 0;
				ci[3] = solution.get(ci[0]).size()-1;
				tail -= routeCount;
				destinations[tail][ccs[routes[tail]]] = routes[i];
			} else {
				ci[0] = routes[tail];
				ci[1] = 0;
				ci[2] = 1;
				ci[3] = cuts[ci[0]][0];
				destinations[tail][0] = routes[i];
			}

			if (ci[1] == ci[3]) assignedChunks[i]--;
		}
	}




	@SuppressWarnings("unchecked")
	@Override
	public void apply() {
		if (deadMove) return;

		ArrayList<Integer> newRoutes[] = new ArrayList[routeCount];


		//Construct new routes
		for (int i = 0; i<routeCount; i++) {
			final int destRoute = i;
			int[][] chunksForRoute = chunks[i];

			newRoutes[i] = new ArrayList<Integer>();
			newRoutes[i].add(0);

			for (int j = 0; j<assignedChunks[i]; j++) {
				int[] chunk = chunksForRoute[j];
				final int srcRoute = chunk[0];
				final int beforeCut = chunk[1];
				final boolean flip = chunk[2] != 0;
				final int lastInCut = chunk[3];

				final Route srcRouteObj = solution.get(srcRoute);
				final int nrSize = newRoutes[destRoute].size();

				newRoutes[destRoute].addAll(
						srcRouteObj.subList(beforeCut+1, lastInCut+1)
				);

				if (flip) Collections.reverse(newRoutes[destRoute].subList(nrSize, newRoutes[destRoute].size()));
			}
		}


		//now copy the new routes over the old routes
		for (int i = 0; i<routeCount; i++) {
			final Route r = solution.get(routes[i]);
			r.clear();
			r.addAll(newRoutes[i]);
		}
	}

	private int routePosition(int x) {
		if (x <= 0) {
			return 0;
		} else {
			return solution.routePosition(x);
		}
	}

	private int routeIndex(int x) {
		if (x <= 0) {
			return -x;
		} else {
			return solution.routeContaining(x);
		}
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public String toString() {
		return "RecombiningMove ("+tag+") [vertices=" + Arrays.toString(vertices) + "] cutting routes " + Arrays.toString(routes);
	}

	public String toLongString() {
		StringBuilder sb = new StringBuilder();
		sb.append(toString());

		sb.append("\nRoutes and Cuts:\n");

		for (int r = 0; r<routeCount ; r++) {
			sb.append(routes[r] + ": ");
			Route r_ = solution.get(routes[r]);
			for (int i = 0, c = 0; i<r_.size(); i++) {
				sb.append(r_.at(i) + " ");
				if (c < ccs[r_.index] && i == cuts[r_.index][c]) {
					sb.append("/ ");
					c++;
				}
			}
			sb.append("\n");
		}

		sb.append("Recombined Routes:\n");
		for (int r = 0; r<routeCount; r++) {
			sb.append(routes[r] + ": ");


			for (int i = 0; i<assignedChunks[r]; i++) {
				int[] chunk = chunks[r][i];
				sb.append((chunk[2] == 0 ? "" : "flipped ") + "cut [" + chunk[1] + "-"+chunk[3]+"] of " + chunk[0]+ ", ");
			}

			sb.append("\n");
		}
		return sb.toString();
	}

	public IRouteModification[] getChanges() {
		if (deadMove) {
			return new IRouteModification[0];
		}

		IRouteModification[] rv = new IRouteModification[routeCount];
		for (int i = 0; i<routeCount; i++) {
			rv[i] = new RecombiningModification(this, i);
		}
		return rv;
	}
}
