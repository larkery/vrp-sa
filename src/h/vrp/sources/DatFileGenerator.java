package h.vrp.sources;

import h.options.DoubleParser;
import h.options.IntegerParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.Options;
import h.options.StringParser;
import h.sexp.ISexpEvaluator;
import h.sexp.SexpParser;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

public class DatFileGenerator {
	private static final String O_INSTANCE_SIZE = "size";
	private static final String O_INSTANCE_RADIUS = "radius";
	private static final String O_INSTANCE_MAXLENGTH = "maxlength";
	private static final String O_INSTANCE_CAPACITY = "capacity";
	private static final String O_INSTANCE_DEMANDS = "demands";
	private static final String O_INSTANCE_VEHICLES = "vehicles";
	private static final String O_INSTANCE_NAME = "name";
	private static final String O_INSTANCE_COMMENT = "comment";
	private static final String O_OUTPUT_FILE = "output";

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Options options = new Options("--");
		
		options.addOption(O_INSTANCE_SIZE, "Number of points in the instance, including depot", new IntegerParser(1000));
		options.addOption(O_INSTANCE_RADIUS, "Radius of instance's circle", new DoubleParser(20));
		options.addOption(O_INSTANCE_DEMANDS, 
				"How to generate the demands; (equal) or (uniform scale)", new StringParser("(equal)"));
		options.addOption(O_INSTANCE_CAPACITY, "What the capacity constraint for this instance should be", new IntegerParser(10));
		options.addOption(O_INSTANCE_MAXLENGTH, "What the maximum route length should be, as a multiple of the radius", new DoubleParser(2));
		options.addOption(O_INSTANCE_VEHICLES, "Suggested number of vehicles for the instance's solution", new IntegerParser(5));
		options.addOption(O_INSTANCE_NAME, "Instance name (default is R+params+datetime)", new StringParser());
		options.addOption(O_INSTANCE_COMMENT, "Instance comment", new StringParser("randomly generated"));
		options.addOption(O_OUTPUT_FILE, "Output file (defaults to stdout)", new StringParser());
		
		try {
			options.parse(args);
			int size = (Integer) options.getOption(O_INSTANCE_SIZE);
			double radius = (Double) options.getOption(O_INSTANCE_RADIUS);
			final int capacity = (Integer) options.getOption(O_INSTANCE_CAPACITY);
			double maxlength = ((Double) options.getOption(O_INSTANCE_MAXLENGTH)).intValue();
			int vehicles = options.getOption(O_INSTANCE_VEHICLES);
			final Random random = new Random();
			
			
			String comment = options.getOption(O_INSTANCE_COMMENT);
			
			IDemandGenerator dg = (IDemandGenerator) SexpParser.evaluateString((String) options.getOption("demands"), 
					new ISexpEvaluator() {
						@Override
						public Object evaluateList(List<Object> results) {
							if (results.get(0).equals("equal")) return new IDemandGenerator() {
								@Override
								public int generateDemand() {
									return 1 ;
								}
								public String toString() {
									return "c";
								}
							};
							if (results.get(0).equals("uniform")) {
								final int x = (int) (((Double) results.get(1)) * capacity);
								return new IDemandGenerator() {
									@Override
									public int generateDemand() {
										return random.nextInt(x) + 1;
									}
									public String toString() {
										return "u" + x;
									}
								};
							}
							return null;
						}
						
						@Override
						public Object evaluateAtom(Object sexp) {
							return sexp;
						}
					});
			
			String name;
			if (options.hasOption(O_INSTANCE_NAME)) {
				name = options.getOption(O_INSTANCE_NAME);
			} else {
				name = "R" + size + "-c" + capacity + "-l" + maxlength + "-r" + radius + "-d" + dg.toString(); 
			}
			
			
			PrintStream out = System.out;
			if (options.hasOption(O_OUTPUT_FILE)) {
				String ofn = ((String) options.getOption(O_OUTPUT_FILE)).replace("$NAME", name);
				System.err.println("Output: " + ofn);
				out = new PrintStream(new File(
				ofn
				));
			}
			
			pp("NAME", name, out);
			pp("COMMENT", comment, out);
			pp("TYPE", "DCVRP", out);
			pp("DIMENSION", ""+size, out);
			pp("EDGE_WEIGHT_TYPE", "EUC_2D", out);
			pp("CAPACITY", "" + capacity, out);
			pp("MAXLENGTH", ""+new Double(maxlength*radius).intValue(), out);
			pp("VEHICLES", ""+vehicles, out);
			out.println("NODE_COORD_SECTION");
			out.println(" 1 0 0");
			for (int i = 2; i<=size; i++) {
				Point2D.Float pt = randomCircularPoint((float) radius, random);
				out.println(" " + i + " " + pt.x + " " + pt.y);
			}
			out.println("DEMAND_SECTION");
			out.println(" 1 0");
			
			for (int i = 2; i<=size; i++) {
				out.println(" " + i  + " " + dg.generateDemand());
			}
			
			out.println("DEPOT_SECTION");
			out.println(" 1");
			out.println("EOF");
		} catch (InvalidOptionException e) {
			System.err.println(e);
			System.err.println(options.getHelp());
		} catch (InvalidArgumentException e) {
			System.err.println(e);
			System.err.println(options.getHelp());
		}
		
	}
	
	static void pp(String k, String v, PrintStream out) {
		out.println(k + " : " + v);
	}
	
	interface IDemandGenerator {
		public int generateDemand();
	}
	
	protected static Point2D.Float randomCircularPoint(float radius, Random random) {
		float r = (float) (radius * Math.sqrt(random.nextFloat()));
		float theta = (float) (random.nextFloat() * Math.PI * 2);
		float x, y;
		x = (float) (r*Math.cos(theta));
		y = (float) (r*Math.sin(theta));
		return new Point2D.Float(x, y);
	}
}
