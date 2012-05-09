package h.vrp.model.evaluation;

public interface ITest {
	boolean isFeasible();

	String getName();

	float getSlackness();
}
