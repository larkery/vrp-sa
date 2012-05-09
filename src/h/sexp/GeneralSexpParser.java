package h.sexp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handy generalised sexp evaluator...
 * @author hinton
 *
 */
public class GeneralSexpParser implements ISexpEvaluator {
	private ISexpEvaluator delegate;

	public GeneralSexpParser() {
		this(new ISexpEvaluator() {
			@Override
			public Object evaluateList(List<Object> results) {
				return "This should never happen";
			}
			
			@Override
			public Object evaluateAtom(Object sexp) {
				return sexp;
			}
		});
	}
	
	public GeneralSexpParser(ISexpEvaluator delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public Object evaluateAtom(Object sexp) {
		return delegate.evaluateAtom(sexp);
	}

	@Override
	public Object evaluateList(List<Object> results) {
		Object rv = null;
		String head = (String) results.get(0);
		if (head.equals("list")) {
			List<Object> rest = new ArrayList<Object>(results.size()-1);
			rest.addAll(results.subList(1, results.size()));
			rv = rest;
		} else if (head.equals("map")) {
			Map<Object, Object> rest = new HashMap<Object, Object>();
			for (int i = 1; i<results.size(); i+=2) {
				rest.put(results.get(i), results.get(i+1));
			}
			rv = rest;
		} else if (head.equals("range")) {
			List<Double> values = new ArrayList<Double>();
			double min, max, step;
			min = doublify(results.get(1));
			max = doublify(results.get(2));
			step = doublify(results.get(3));
			
			while (min < max + step) {
				values.add(min);
				min += step;
			}
			rv = values;
		} else {
			return delegate.evaluateList(results);
		}
		return rv;
	}
	Double doublify(Object o) {
		if (o instanceof Double) {
			Double d = (Double) o;
			return d;
		} else if (o instanceof Integer) {
			Integer i = (Integer) o;
			return i.doubleValue();
		}
		return null;
	}
}
