package h.exp.compare;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Iterator;

public class Interpolator {
	private Iterator<Double> iter;
	Point2D.Double p1, p2;
	public Interpolator(Iterator<Point2D.Double> iter) {
		this.iter = iter;
		p1 = iter.next();
		p2 = iter.next();
	}
	
	public double valueAt(double x) {
		wind(x);
		if (x < p1.x) {
			return p1.y;
		}
		if (x > p2.x){
			return p2.y;
		}
		return interpolate(x);
	}

	private double interpolate(double x) {
		if (x == p1.x) return p1.y;
		if (x == p2.x) return p2.y; 
		return p1.y + (p2.y - p1.y) * ((x-p1.x) / (p2.x-p1.x));
	}

	private void wind(double x) {
		while (x > p2.x && iter.hasNext()) {
			p1 = p2;
			p2 = iter.next();
		}
	}
}