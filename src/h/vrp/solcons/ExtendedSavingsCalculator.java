package h.vrp.solcons;

import h.vrp.model.Instance;
import h.vrp.solcons.SavingsConstructor.ISavingsCalculator;
import h.vrp.solcons.SavingsConstructor.SavingsMove;

public class ExtendedSavingsCalculator implements ISavingsCalculator {
	private float ffdWeight;
	private float radialWeight;
	private float addedEdgeWeight;
	
	public ExtendedSavingsCalculator(float addedEdgeWeight, float radialWeight, float ffdWeight) {
		this.addedEdgeWeight = addedEdgeWeight;
		this.radialWeight = radialWeight;
		this.ffdWeight = ffdWeight;
	}
	@Override
	public float calculateSavings(Instance instance, SavingsMove move) {
		final int t = move.r1.get(-1);
		final int h = move.r2.get(1);

		final float rt = instance.getEdgeCost(t, 0);
		final float rh = instance.getEdgeCost(0, h);
		
		return (rt + rh) -
			(addedEdgeWeight * instance.getEdgeCost(t, h)) + //anti-circumference penalty 
			(radialWeight * Math.abs(rt - rh)) + //radial difference bonus
			(ffdWeight * (instance.getVertexDemand(t) + instance.getVertexDemand(h)) / instance.getMeanVertexDemand());
	}
	public static ISavingsCalculator createInstance(
			double lambda, double mu, double nu) {
		if (lambda == 1 && mu == 0 && nu == 0) {
			return SavingsConstructor.normalSavingsCalculator;
		} else {
			return new ExtendedSavingsCalculator((float) lambda, (float) mu,(float) nu);
		}
	}
}
