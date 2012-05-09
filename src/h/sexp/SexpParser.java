package h.sexp;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SexpParser {
	public static Object parse(String sexp) throws IOException {
		StreamTokenizer st = new StreamTokenizer(new StringReader(sexp));
		st.commentChar(';');
		/* WTF, streamtokenizer? WTF! */
		st.ordinaryChars('0', '9');
		st.ordinaryChar('-');
		st.ordinaryChar('.');
		st.wordChars('0', '9');
		st.wordChars('-', '-');
		st.wordChars('.', '.');
		st.wordChars('*', '*');
		st.wordChars('+', '+');
		st.wordChars('/', '/');
		st.quoteChar('"');
		st.ordinaryChar('\'');
		st.wordChars('_', '_');
		return parse(st);
	}

	private static Object parse(StreamTokenizer st) throws IOException {
		int token = st.nextToken();
		switch (token) {
		case StreamTokenizer.TT_EOF: return null;
		case StreamTokenizer.TT_NUMBER: return number(st.sval);
		case StreamTokenizer.TT_WORD: return atom(st.sval);
		case '"': return st.sval;
		case '(':
			List<Object> clunge = new ArrayList<Object>();
			Object o;
			while ((o = parse(st)) != null) {
				clunge.add(o);
			}
			return clunge;
		case ')':
			return null;
		}
		
		return null;
	}

	private static Object atom(String sval) {
		try {
			return number(sval);
		} catch (NumberFormatException nfe) {
			if (sval.equalsIgnoreCase("true")) {
				return new Boolean(true);
			} else if (sval.equalsIgnoreCase("false")) {
				return new Boolean(false);
			}
			return sval;
		}
	}
	
	private static Object number(String sval) {
		try {
			return new Integer(sval);
		}catch (NumberFormatException nfe) {
			return new Double(sval);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Object evaluate(Object sexp, ISexpEvaluator evaluator) {
		if (sexp instanceof List) {
			List<Object> objs = (List<Object>) sexp;
			List<Object> results = new ArrayList<Object>();
			for (int i = 0; i<objs.size(); i++) {
				results.add(evaluate(objs.get(i), evaluator));
			}
			return evaluator.evaluateList(results);
		} else {
			return evaluator.evaluateAtom(sexp);
		}
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println(evaluate(parse(args[0]),
				new ISexpEvaluator() {

					@Override
					public Object evaluateAtom(Object sexp) {
						return sexp;
					}

					@Override
					public Object evaluateList(List<Object> results) {
						StringBuilder sb = new StringBuilder();
						sb.append("( ");
						for (Object o : results) {
							if (o instanceof String)
								sb.append("\"" + o.toString().replace("\"", "\\\"") +"\"");
							sb.append(" ");
						}
						sb.append(")");
						return sb.toString();
					}}));
				;
	}

	public static Object evaluateString(String option,
			ISexpEvaluator eval) {
		try {
			return evaluate(parse(option), eval);
		} catch (IOException e) {
			return null;
		}
	}
}
