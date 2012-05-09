package h.vrp.runner;

import h.util.timing.Timer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JPanel;

public class TracePanel extends JPanel {
	ArrayList<Point2D.Float> points = new ArrayList<Point2D.Float>();
	Rectangle2D.Float boundingBox = new Rectangle2D.Float();
	Timer t = Timer.getInstance();
	public TracePanel() {
		super();
	}
	
	public void reset() {
		points.clear();
		t.reset();
		boundingBox = new Rectangle2D.Float();
	}
	
	public void addPoint(float pt) {
		Point2D.Float pt2 = new Point2D.Float((float) t.getElapsedTime(), pt);
		points.add(pt2);
		if (pt2.x < boundingBox.x) {
			boundingBox.x = pt2.x;
		} else if (boundingBox.width < (pt2.x - boundingBox.x)) {
			boundingBox.width = pt2.x - boundingBox.x;
		}
		
		if (pt2.y < boundingBox.y) {
			boundingBox.y = pt2.y;
		} else if (boundingBox.height < (pt2.y - boundingBox.y)) {
			boundingBox.height = pt2.y - boundingBox.y;
		}

		this.repaint();
	}
	
	protected void paintComponent(Graphics g_) {
		super.paintComponent(g_);
		
		Graphics2D g = (Graphics2D) g_;
		final int w = getWidth();
		final int h = getHeight();
		final int d = Math.min(w, h);
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		
	}
}
