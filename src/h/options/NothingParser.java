package h.options;


import java.util.Iterator;

public class NothingParser implements OptionParser {
	public Object parse(String op, Iterator<String> iter) throws InvalidArgumentException {
		return true;
	}

	public Object getDefaultValue() {
		return false;
	}

	public boolean hasDefaultValue() {
		// TODO Auto-generated method stub
		return true;
	}
}
