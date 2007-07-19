package scikit.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.JComponent;

import scikit.util.Bounds;


public class GraphicsAWT implements Graphics {
	private final Graphics2D engine;
	private final Scene scene;
	private final Bounds pixBds;
	private Bounds datBds;
	
	public GraphicsAWT(Graphics2D engine, Scene scene) {
		this.engine = engine;
		this.scene = scene;
		datBds = pixBds = scene.pixelBounds();
	}
	
	public Object engine() {
		return engine;
	}

	public Scene scene() {
		return scene;
	}

	public void projectOrtho2D(Bounds proj) {
		// the internal projection is a shift and scale from the "proj" rectangle 
		this.datBds = proj;
	}

	private int transX(double x) {
		return (int) (pixBds.xmax * (x - datBds.xmin) / datBds.getWidth());
	}
	
	private int transY(double y) {
		return (int) (pixBds.getHeight() - pixBds.ymax * (y - datBds.ymin) / datBds.getHeight());
	}
	
	private int offsetX(double w) {
		return (int) (w * pixBds.getWidth() / datBds.getWidth());
	}
	
	private int offsetY(double h) {
		return (int) (h * pixBds.getHeight() / datBds.getHeight());
	}
	
	public void setColor(Color color) {
		engine.setColor(color);
	}
	
	public void drawPoint(double x, double y) {
		int pix = 4;
		engine.fillRect((int)transX(x)-pix/2, (int)transY(y)-pix/2, pix, pix);
	}
	
	public void drawLine(double x1, double y1, double x2, double y2) {
		engine.drawLine(transX(x1), transY(y1), transX(x2), transY(y2));
	}

	public void drawLines(double[] xys) {
		for (int i = 2; i < xys.length; i += 2)
			drawLine(xys[i-2], xys[i-1], xys[i+0], xys[i+1]);
	}

	public void drawRect(double x, double y, double w, double h) {
		engine.drawRect(transX(x), transY(y)-offsetY(h), offsetX(w), offsetY(h));
	}

	public void fillRect(double x, double y, double w, double h) {
		engine.fillRect(transX(x), transY(y)-offsetY(h), offsetX(w), offsetY(h));
	}
	
	public void drawCircle(double x, double y, double r) {
		int w = offsetX(2*r);
		int h = offsetY(2*r);
		engine.drawOval(transX(x)-w/2, transY(y)-h*3/2, w, h);
	}

	public void fillCircle(double x, double y, double r) {
		int w = offsetX(2*r);
		int h = offsetY(2*r);
		engine.fillOval(transX(x)-w/2, transY(y)-h*3/2, w, h);
	}

	public double stringWidth(String str) {
		double pix2dat = datBds.getWidth() / pixBds.getWidth();
		return engine.getFontMetrics().getStringBounds(str, engine).getWidth() * pix2dat;
	}
	
	public double stringHeight(String str) {
		double pix2dat = datBds.getHeight() / pixBds.getHeight();
		double fudge = 0.7;
		return engine.getFontMetrics().getStringBounds(str, engine).getHeight() * pix2dat * fudge;
	}

	public void drawString(String str, double x, double y) {
		engine.drawString(str, transX(x), transY(y));
	}

	public static JComponent createComponent(final Scene scene) {
		final JComponent component = new JComponent() {
			private static final long serialVersionUID = 1L;
			public void paintComponent(java.awt.Graphics engine) {
				Graphics g = new GraphicsAWT((Graphics2D)engine, scene);
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.projectOrtho2D(scene.dataBounds());
				for (Drawable d : scene.allDrawables())
					d.draw(g);
			}
		};
		component.setPreferredSize(new Dimension(300, 300));
		return component;
	}
}
