package h.vrp.solcons;

import java.util.List;

import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;
import h.util.random.HintonRandom;

public class SolConSpecParser {
	public static ISolutionConstructor parseSpec(String spec, final HintonRandom random) {
		return (ISolutionConstructor) SexpParser.evaluateString(spec, new ISexpEvaluator() {
			@Override
			public Object evaluateList(List<Object> list) {
				String head = (String) list.get(0);
				if (head.equals("savings")) {
					return new SavingsConstructor();
				} else if (head.equals("random")) {
					boolean fullyRandom = (Boolean) list.get(1);
					boolean feasible = (Boolean) list.get(2);
					return new RandomConstructor(random, fullyRandom, feasible);
				} else {
					System.err.println("Unknown solution constructor: " + head);
				}
				return null;
			}
			
			@Override
			public Object evaluateAtom(Object sexp) {
				return sexp;
			}
		});
	}
}
