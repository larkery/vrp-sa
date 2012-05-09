package h.options;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Options {
	protected String optionPrefix;
	protected Map<String, OptionParser> parsers;
	protected Map<String, Object> results;
	
	StringBuilder allHelp = new StringBuilder();
	
	public Options(String optionPrefix) {
		this.optionPrefix = optionPrefix;
		this.parsers = new HashMap<String, OptionParser>();
		this.results = new HashMap<String, Object>();
	}
	
	public Options() {
		this("--");
	}
	
	public void addOption(String name, OptionParser parser) {
		addOption (name, "undocumented", parser);
	}
	
	public void addOption(String[] names, OptionParser parser) {
		addOption(names, "undocumented", parser);
	}
	
	public void addOption(String[] names, String help, OptionParser parser) {
		for (String name : names) {
			parsers.put(name, parser);
		}
		if (help != null) {
			allHelp.append("\t --" + names[0] + " : " + help);
			if (parser.hasDefaultValue())
				allHelp.append(" (default value : " + parser.getDefaultValue() +")");
			else
				allHelp.append(" (no default value)");
			if (names.length > 1) {
				allHelp.append(" (synonyms : ");
				for (int x = 1; x < names.length; x++) {
					allHelp.append(names[x]);
					if (x > 1) allHelp.append(", ");
				}
				allHelp.append(")");
			}
			allHelp.append("\n");
		}
	}
	
	public String getHelp() {
		return allHelp.toString();
	}
	
	public void addOption(String name, String help, OptionParser parser) {
		addOption(new String[] {name}, help, parser);
	}
	
	//TODO cleanup
	@SuppressWarnings("unchecked")
	public <T> T getOption(String s) {
		if (results.containsKey(s)) {
			return (T) results.get(s);
		} else if (parsers.containsKey(s)) {
			if (parsers.get(s).hasDefaultValue())
				return (T) parsers.get(s).getDefaultValue();
			else
				return (T) results.get(s);
		} else {
			return (T) results.get(s);
		}
	}
	
	public List<String> parse(List<String> args) 
		throws  InvalidOptionException, InvalidArgumentException
	{
		Iterator<String> it = args.iterator();
		List<String> spare = new ArrayList<String>();
		while (it.hasNext()) {
			String arg = it.next();
			if (arg.startsWith(optionPrefix)) {
				//split off the prefix and lookup the parser
				String opt = arg.substring(optionPrefix.length());
				if (parsers.containsKey(opt)) {
					OptionParser parser = parsers.get(opt);
					results.put(opt, parser.parse(opt, it));
				} else {
					throw new InvalidOptionException(arg);
				}
			} else {
				spare.add(arg);
				while (it.hasNext())
					spare.add(it.next());
			}
		}
		return null;
	}

	public List<String> parse(String[] args) throws InvalidOptionException, InvalidArgumentException {
		return parse(Arrays.asList(args));
	}
	
	public boolean hasOption(String s) {
		if (results.containsKey(s))
			return true;
		else 
			return parsers.containsKey(s) && parsers.get(s).hasDefaultValue();
	}

	private final String quoteify(Object o) {
		return "\"" + o.toString().replace("\"", "\\\"") + "\"";
	}
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("{");
		
		boolean b = false;
		for (Map.Entry<String, Object> e : results.entrySet()) {
			if (b) sb.append(", ");
			b = true;
			sb.append("\n");
			sb.append("    ");
			sb.append(quoteify(e.getKey()) + ":" + quoteify(e.getValue()));
			
		}
		
		sb.append("\n}");
		
		return sb.toString();
	}
	
	
}