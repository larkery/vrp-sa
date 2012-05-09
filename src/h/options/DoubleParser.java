package h.options;

import java.util.Iterator;

public class DoubleParser implements OptionParser {
	private Double defaultValue = null;

	public DoubleParser() {
	}
	public DoubleParser(double value) {
		this.defaultValue = value;
	}
	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean hasDefaultValue() {
		return defaultValue != null;
	}

	@Override
	public Object parse(String op, Iterator<String> iter)
			throws InvalidArgumentException {
		String s = iter.next();
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			throw new InvalidArgumentException("Are you sure " + s + " is a double?");
		}
	}

}
