package h.options;


import java.util.Arrays;
import java.util.Iterator;

public class StringListParser implements OptionParser {
	protected String separator;
	protected String defaultValue;
	public StringListParser(String separator, String defaultValue) {
		this.defaultValue = defaultValue;
		this.separator = separator;
	}
	public StringListParser(String separator) {
		this(separator, null);
	}
	public Object parse(String op, Iterator<String> iter) throws InvalidArgumentException {
		String s = iter.next();
		String [] args = s.split(separator);
		return Arrays.asList(args);
	}
	public Object getDefaultValue() {
		if (defaultValue != null) {
			return Arrays.asList(defaultValue.split(separator));
		} else {
			return null;
		}
	}
	public boolean hasDefaultValue() {
		return defaultValue != null;
	}
}
