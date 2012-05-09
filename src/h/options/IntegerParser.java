package h.options;


import java.util.Iterator;

public class IntegerParser implements OptionParser {
	protected Integer defaultValue = null;
	public IntegerParser(Integer defaultValue) {
		this.defaultValue = defaultValue;
	}

	
	public IntegerParser() {
		
	}

	
	public Object getDefaultValue() {
		return defaultValue;
	}

	public boolean hasDefaultValue() {
		return defaultValue != null;
	}

	public Object parse(String op, Iterator<String> iter) throws InvalidArgumentException {
		try {
			String s = iter.next();
			Integer i = Integer.valueOf(s);
			return i;
		} catch (Exception e) {
			throw new InvalidArgumentException();
		}
	}

}
