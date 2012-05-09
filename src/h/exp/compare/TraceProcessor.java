package h.exp.compare;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TraceProcessor {
	public interface IReducer {
		public double reduce(double x, double[] ys);
	}
	
	public static List<Double> processTraces(List<Iterator<Point2D.Double>> iterators, int samples, double maxX, IReducer reducer) {
		List<Interpolator> interpolators = new ArrayList<Interpolator>(iterators.size());
		List<Point2D.Double> results = new ArrayList<Point2D.Double>(samples);
		for (Iterator<Point2D.Double> iter : iterators) interpolators.add(new Interpolator(iter));
		double[] ys = new double[iterators.size()];
		
		double point = 0;
		final double increment = maxX / samples;
		while (samples > 0) {
			for (int i = 0; i<ys.length; i++) {
				ys[i] = interpolators.get(i).valueAt(point);
			}
			results.add(new Point2D.Double(point, reducer.reduce(point, ys)));
			point += increment;
			samples--;
		}
		return results;
	}
	
	public static IReducer mean = new IReducer() {
		@Override
		public double reduce(double x, double[] ys) {
			double d = 0;
			for (double s : ys) d+=s;
			return d/ys.length;
		}
	};
	
	public static IReducer min = new IReducer() {
		@Override
		public double reduce(double x, double[] ys) {
			double r = java.lang.Double.MAX_VALUE;
			for (double y : ys) r = Math.min(r, y);
			return r;
		}
	};
	
	public static IReducer ratio = new IReducer() {
		@Override
		public double reduce(double x, double[] ys) {
			return ys[0] / ys[1];
		}
	};
}
