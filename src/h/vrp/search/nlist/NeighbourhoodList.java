package h.vrp.search.nlist;

import h.vrp.model.Instance;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Unimaginably noddy implementation of the neighbourhood list
 * Does the job though
 * @author hinton
 *
 */
public class NeighbourhoodList {
	class DistanceComparator implements Comparator<Integer> {
		Instance instance;
		int source;
		@Override
		public int compare(Integer o1, Integer o2) {
			float d1 = instance.getEdgeCost(source, o1);
			float d2 = instance.getEdgeCost(source, o2);
			if (d1 < d2) return -1;
			else if (d2 < d1) return 1;
			return 0;
		}
		public DistanceComparator(Instance instance) {
			super();
			this.instance = instance;
		}
		public void setSource(int source) {
			this.source = source;
		}
	}
	int[][] neighbours;
	public NeighbourhoodList(Instance instance, int listSize) {
		final int pointCount = instance.getPoints().size();
		if (listSize > pointCount)
			listSize = pointCount-1;
		neighbours = new int[pointCount][listSize];
		Integer[] allPoints = new Integer[pointCount];
		for (int i = 0; i<pointCount; i++)
			allPoints[i] = i;
		
		DistanceComparator dc = new DistanceComparator(instance);
		
		for (int i = 0; i<pointCount; i++) {
			dc.setSource(i);
			Arrays.sort(allPoints, dc);
			assert(allPoints[0] == i);
			for (int p = 0; p<listSize; p++) {
				neighbours[i][p] = allPoints[p+1];
			}
		}
	}
	public int[] getNeighbours(int x) {
		return neighbours[x];
	}
}
