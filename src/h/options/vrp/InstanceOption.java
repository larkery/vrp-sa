package h.options.vrp;

import java.util.Iterator;

import h.options.InvalidArgumentException;
import h.options.OptionParser;
import h.options.Options;
import h.vrp.sources.SourceSpecParser;

public class InstanceOption implements OptionParser {
	public static final String HELP = "An instance source; currently supports (file path/to/file.dat)";
	public static final String DEFAULT_OPTION = "source";

	public InstanceOption() {
		
	}
	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public boolean hasDefaultValue() {
		return false;
	}

	@Override
	public Object parse(String op, Iterator<String> iter)
			throws InvalidArgumentException {
		return SourceSpecParser.parseSexp(iter.next()).createInstance();
	}
	public static void addTo(Options options) {
		options.addOption(DEFAULT_OPTION, HELP, new InstanceOption());
	}
}
