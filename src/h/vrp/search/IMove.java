package h.vrp.search;

import h.vrp.model.IRouteModification;

public interface IMove {
	void apply();
	boolean verify();
	
	IRouteModification[] getChanges();
}
