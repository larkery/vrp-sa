package h.vrp.stochasticsavings;

import h.options.DoubleParser;
import h.options.InvalidArgumentException;
import h.options.InvalidOptionException;
import h.options.Options;
import h.options.vrp.InstanceOption;
import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;
import h.vrp.model.evaluation.DeltaEvaluator;
import h.vrp.solcons.ExtendedSavingsCalculator;
import h.vrp.solcons.SavingsConstructor;

import java.util.List;
import java.util.Map;

public class GetInstanceDetails {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GetInstanceDetails me = new GetInstanceDetails();
		Options options = new Options("--");
		
		InstanceOption.addTo(options);
		options.addOption("lambda", new DoubleParser(1));
		options.addOption("mu", new DoubleParser(1));
		options.addOption("nu", new DoubleParser(1));
		
		try {
			options.parse(args);
		} catch (InvalidOptionException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		
		Instance instance = options.getOption(InstanceOption.DEFAULT_OPTION);
		instance.setUsingHardConstraints(true);
		CSVBuilder fields = me.new CSVBuilder();
		fields.add(instance);
		fields.add(instance.getPoints().size());
		fields.add(instance.getCapacity());
		fields.add(instance.getMaxLength());
		fields.add(instance.getMeanVertexDemand());
		fields.add(instance.getMeanEdgeLength());
		
		Solution solution = new SavingsConstructor().createSolution(instance);
		DeltaEvaluator de = new DeltaEvaluator(instance, solution);
		//get constraint tightness numbers
		Map<String, List<Float>> slackness = de.getSlacknesses();
		
		fields.add("cw");
		for (Map.Entry<String, List<Float>> e : slackness.entrySet()) {
			fields.add(e.getKey());
			float T = 0;
			for (float f : e.getValue()) {
				T += f;
			}
			T /= e.getValue().size();
			fields.add(T);
		}
		
		float lambda = ((Double)options.getOption("lambda")).floatValue();
		float mu = ((Double)options.getOption("mu")).floatValue();
		float nu = ((Double)options.getOption("nu")).floatValue();
		Solution esolution = new SavingsConstructor(new ExtendedSavingsCalculator(lambda, mu, nu)).createSolution(instance);
		
		de = new DeltaEvaluator(instance, esolution);
		Map<String, List<Float>> slackness2 = de.getSlacknesses();
		fields.add("altinel");
		for (Map.Entry<String, List<Float>> e : slackness2.entrySet()) {
			fields.add(e.getKey());
			float T = 0;
			for (float f : e.getValue()) {
				T += f;
			}
			T /= e.getValue().size();
			fields.add(T);
		}
		
		//check the pair differences
		int[] differences = new int[instance.getPoints().size()];
		for (int i = 1; i<instance.getPoints().size(); i++) {
			for (int j = 1; j<i; j++) {
				if (j == i) continue;
				if (solution.routeContaining(i) == solution.routeContaining(j)) {
					if (esolution.routeContaining(i) != esolution.routeContaining(j)) {
						differences[i]++;
					}
				}
			}
		}
		
		int mc = 0;
		for (int i = 1; i<differences.length; i++) {
			if (differences[i] > solution.get(solution.routeContaining(i)).size()/2) {
//				System.err.println("Vertex " + i + " kinda moved");
				mc++;
//			} else if (differences[i] > 0) {
//				System.err.println("Vertex " + i + " has " + differences[i]);
			} else {
//				System.err.println("Vertex " + i + " has not lost any neighbours");
			}
		}
		
//		System.err.println(mc + " moved vertices total (" + ((double)  100 * mc / (differences.length - 1) ) + "%)");
		fields.add(mc);
		
		fields.add(stringify(solution));
		fields.add(stringify(esolution));
		
		System.out.println(fields.toString());
	}
	
	static String stringify(Solution sol) {
		StringBuffer sb = new StringBuffer();
		for (Route route : sol) {
			if (route.size() == 1) continue;
			sb.append("(");
			for (int i = 0; i<route.size(); i++) {
				sb.append(route.get(i) + " ");
			}
			sb.append(") ");
		}
		return sb.toString();
	}
	
	class CSVBuilder {
		private StringBuilder sb;

		public CSVBuilder() {
			this.sb = new StringBuilder();
		}
		
		public void add(Object o) {
			add(o.toString());
		}
		
		public void add(int x) {
			add("" + x);
		}
		
		public void add(float meanVertexDemand) {
			add(""+ meanVertexDemand);
		}

		public void add(double d) {
			add("" + d);
		}
		
		public void add(String s) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(s);
		}
		public String toString() {
			return sb.toString();
		}
	}
}
