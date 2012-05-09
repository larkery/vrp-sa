package h.options;


import java.util.Iterator;

public class StringEatingParser implements OptionParser {
	protected String defaultValue;
	protected boolean hasDefaultValue;
	public StringEatingParser(String defaultValue) {
		this.defaultValue = defaultValue;
		hasDefaultValue = true;
	}
	public StringEatingParser() {
		hasDefaultValue = false;
	}
	public Object getDefaultValue() {
		return defaultValue;
	}

	public boolean hasDefaultValue() {
		return hasDefaultValue;
	}

	public Object parse(String op, Iterator<String> iter)
			throws InvalidArgumentException {
		StringBuffer sb = new StringBuffer();
		while (iter.hasNext()) {
			sb.append(iter.next());
			sb.append(" ");
		}
		return sb.toString();
	}
}
