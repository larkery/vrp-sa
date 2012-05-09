package h.vrp.search;

import java.util.ArrayList;
import java.util.List;

import h.vrp.model.Solution;

public abstract class MultiNeighbourhood implements INeighbourhood {
	protected List<INeighbourhood> subNeighbourhoods = new ArrayList<INeighbourhood>();

	public void addNeighbourhood(INeighbourhood n) {
		subNeighbourhoods.add(n);
	}
	
	@Override
	public int size(Solution solution) {
		int total = 0;
		for (INeighbourhood n : subNeighbourhoods)
			total += n.size(solution);
		return total;
	}
}
