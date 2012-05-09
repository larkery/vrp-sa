package h.vrp.runner;

import h.vrp.model.Instance;
import h.vrp.model.Route;
import h.vrp.model.Solution;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JPanel;

public class SolutionPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2713660033780082994L;
	private Solution solution;
	private Instance instance;
	private List<Point2D.Float> points;
	
	public SolutionPanel(Instance instance, Solution solution) {
		setInstance(instance);
		setSolution(solution);
	}
	
	public void setSolution(Solution solution) {
		this.solution = solution;
//		repaint();
	}
	
	public void setInstance(Instance instance) {
		this.instance = instance;
		this.points = instance == null ? null : instance.getNormalisedPoints();
		System.err.println(points);
//		repaint();
	}

	@Override
	protected void paintComponent(Graphics g_) {
		super.paintComponent(g_);
		
		Graphics2D g = (Graphics2D) g_;
		
		final int w = getWidth();
		final int h = getHeight();
		final int d = Math.min(w, h);
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		if (instance == null) return;
		AffineTransform t = g.getTransform();
		{
			g.translate(w/2, h/2); //0,0 is now in the middle of the window
			g.scale(d, d); //scale so the shortest dimension is now 1 unit
			g.rotate(Math.PI / 2);
			final float pixel = 1.5f/d;
			final float dcRad = 2.0f * pixel;
			final Ellipse2D.Float dc = new Ellipse2D.Float(-dcRad,-dcRad,2*dcRad,2*dcRad);
			
			Font f = g.getFont();
			AffineTransform fontTransform = AffineTransform.getScaleInstance(1.0/d, 1.0/d);
			fontTransform.rotate(-Math.PI/2);
			g.setFont(f.deriveFont(fontTransform));
			
			//draw each route
			g.setStroke(new BasicStroke(pixel));
			if (solution != null) {
				final float hueStep = 1.0f/solution.size();
				
				for (Route r : solution) {
					final Color routeColor = Color.getHSBColor(hueStep * r.index, 1, 1);
					g.setColor(routeColor);
					{
						AffineTransform t2 = g.getTransform();
						g.translate(0.3 - (r.index+1) *hueStep / 3, -0.6);
						g.drawString("Route " + r.index, 0,0);
						g.setTransform(t2);
					}
					
					
					GeneralPath p = new GeneralPath();
					Point2D.Float depot = points.get(r.get(0));
					p.moveTo(depot.x, depot.y);
					for (int i = 1; i<r.size(); i++) {
						final Point2D.Float pt = points.get(r.get(i));
						p.lineTo(pt.x, pt.y);
					}
					p.lineTo(depot.x, depot.y);
					g.draw(p);
				}
			}
			g.setColor(Color.black);
			
			//draw and number vertices
			int ix = 0;
			for (Point2D.Float pt : points) {
				AffineTransform t1 = g.getTransform();
				g.translate(pt.x, pt.y);
				g.draw(dc);
				g.drawString("" + ix, -dcRad, -dcRad);
				
//				g.drawString("dc " + ix, 0, 0);
				ix++;
				g.setTransform(t1);
			}
			g.setFont(f);
		}
		g.setTransform(t);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(500,500);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(1024,768);
	}
	
}
