package h.vrp.sources;

import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Parses expressions like
 * (file "/Users/hinton/some dir/file.dat") -> an instance from a .DAT file
 * (random 10 5 10) -> an instance consisting of random points in a unit circle, with a capacity constraint of 5 and a length constraint of  and equal demand
 * @author hinton
 *
 */
public class SourceSpecParser {
	public static IInstanceFactory parseSexp(String sexp) {
		return (IInstanceFactory) SexpParser.evaluateString(sexp, new ISexpEvaluator() {
			@Override
			public Object evaluateList(List<Object> list) {
				String head = (String) list.get(0);
				if (head.equals("file")) {
					try {
						return new DatFile((String) list.get(1));
					} catch (FileNotFoundException e) {
						System.err.println(e.getMessage());
						return null;
					}
				} else if (head.equals("random")) {
					return new RandomPoints(
							(Integer) list.get(1)
					);
				}
				System.err.println("Unknown type of source");
				return null;
			}
			
			@Override
			public Object evaluateAtom(Object sexp) {
				return sexp;
			}
		});
	}
}
