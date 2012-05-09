package h.sexp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArithmeticEvaluator {
	public static double evaluateSexp(String s, Object[] keyValues) {
		final Map<String, Number> variables = new HashMap<String, Number>();
		for (int i = 0; i<keyValues.length; i+=2) {
			variables.put((String) keyValues[i], (Number)keyValues[i+1]);
		}
		
		return (Double) SexpParser.evaluateString(s, new ISexpEvaluator() {
			@Override
			public Object evaluateList(List<Object> results) {
				String op = (String) results.get(0);
				if (op.equals("*")) {
					double d = 1;
					for (Object o : results.subList(1, results.size())) {
						d *= ((Number)o).doubleValue();
					}
					return (Double) d;
				} else if (op.equals("/")) {
					return (Double)
						((Number)results.get(1)).doubleValue() /
						((Number)results.get(2)).doubleValue();
				} else if (op.equals("-")) {
					if (results.size() == 2) {
						return (Double) (- ((Number)results.get(1)).doubleValue());
					} else {
						return (Double) 
						((Number)results.get(1)).doubleValue() -
						((Number)results.get(2)).doubleValue();
					}
				} else if (op.equals("+")) {
					double d = 0;
					for (Object o : results.subList(1, results.size())) {
						d += ((Number)o).doubleValue();
					}
					return (Double) d;
				}
				return null;
			}
			
			@Override
			public Object evaluateAtom(Object atom) {
				if (atom instanceof String) 
					if (variables.containsKey(atom))
						return variables.get(atom);
				return atom;
			}
		});
	}
}
