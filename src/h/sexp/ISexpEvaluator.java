package h.sexp;

import java.util.List;

public interface ISexpEvaluator {
	public Object evaluateList(List<Object> results);

	public Object evaluateAtom(Object sexp);
}
