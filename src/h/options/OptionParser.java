package h.options;


import java.util.Iterator;

public interface OptionParser {
	//use of iterator is a limitation here
	public Object parse(String op, Iterator<String> iter) throws InvalidArgumentException;
	public boolean hasDefaultValue();
	public Object getDefaultValue();
}
