package h.vrp.search.util;

import java.util.Iterator;
import java.util.List;

import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;
import h.util.random.HintonRandom;
import h.vrp.model.Instance;
import h.vrp.model.Solution;
import h.vrp.search.DelegatingNeighbourhood;
import h.vrp.search.INeighbourhood;
import h.vrp.search.RandomMultiNeighbourhood;
import h.vrp.search.SizeBiasedNeighbourhood;
import h.vrp.search.blind.BlindNeighbourhood;
import h.vrp.search.cl.CLNeighbourhood;
import h.vrp.search.cl.CLNeighbourhood2;
import h.vrp.search.ewr.EdgeWeightedNeighbourhood;
import h.vrp.search.nlist.NLNeighbourhood;
import h.vrp.search.nlist.NeighbourhoodList;
import h.vrp.search.osman.OsmanNeighbourhood;
import h.vrp.search.split.SplitNeighbourhood;

public class NeighbourhoodSpecParser {
	public static INeighbourhood parseSexp(String option, final Instance instance, final Solution solution, final HintonRandom random) {
		return (INeighbourhood) SexpParser.evaluateString(option, new ISexpEvaluator(){
			@Override
			public Object evaluateAtom(Object sexp) {
				return sexp;
			}

			@Override
			public Object evaluateList(List<Object> results) {
				String name = (String) results.get(0);
				if (name.equals("delegate")) {
					DelegatingNeighbourhood dn = new DelegatingNeighbourhood();
					
					for (Object o : results.subList(1, results.size())) {
						dn.addNeighbourhood((INeighbourhood) o);
					}
					
					return dn;
				} else if (name.equals("roulette")) {
					RandomMultiNeighbourhood rn = new RandomMultiNeighbourhood(random);
					Iterator<Object> iterator = results.iterator();
					iterator.next();
					while (iterator.hasNext()) {
						Object o = iterator.next();
						if (o instanceof INeighbourhood) {
							rn.addNeighbourhood((INeighbourhood) o);
						} else {
							double d = 1;
							if (o instanceof Integer) {
								d = (Integer) o;
							} else if (o instanceof Double) {
								d = (Double) o;
							}
							rn.addNeighbourhood((INeighbourhood) iterator.next(), d);
						}
					}
					return rn;
				} else if (name.equals("crossing")) {
					boolean b = false;
					if (results.size() > 1) {
						b = (Boolean) results.get(1);
					}
					if (b) {
						return new CLNeighbourhood2(instance, solution, random);
					} else {
						return new CLNeighbourhood(instance, solution, random);
					}
				} else if (name.equals("edge")) {
					return new EdgeWeightedNeighbourhood(instance, solution, random);
				} else if (name.equals("random")) {
					if (results.size() == 1) {
						return new BlindNeighbourhood(random);
					} else if (results.size() == 3) {
						return new BlindNeighbourhood(random, (Integer) results.get(1), (Integer) results.get(2));
					}
				} else if (name.equals("split")) {
					return new SplitNeighbourhood(random);
				} else if (name.equals("size")) {
					SizeBiasedNeighbourhood n = new SizeBiasedNeighbourhood(random);
					for (Object o : results.subList(1, results.size())) {
						n.addNeighbourhood((INeighbourhood) o);
					}
					return n;
				} else if (name.equals("nlist")) {
					int size = 10;
					if (results.size() > 1) {
						size = (Integer) results.get(1);
					}
					return new NLNeighbourhood(instance, size, random);
				} else if (name.equals("osman")) {
					return new OsmanNeighbourhood(random);
				}
				return null;
			}});
	}
}
