package h.vrp.model.evaluation;

import h.vrp.model.Route;

/**
 * A device which tracks some things related to a route
 * @author hinton
 */
public interface ICalculator {
	public void test(Route r);
	public void accept();
	public void reject();
}
