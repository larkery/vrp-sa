package h.vrp.search;


public interface IOptimiser {
	void setFitnessListener(IFitnessListener trace);
	boolean step();
	boolean finished();
}
