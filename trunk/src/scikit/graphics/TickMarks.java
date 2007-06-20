package scikit.graphics;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import java.awt.Canvas;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import javax.media.opengl.GL;

import com.sun.opengl.util.GLUT;

import scikit.util.Bounds;


public class TickMarks implements Drawable {
	private Canvas _canvas;
	
	private static GLUT glut = new GLUT();
	private static int FONT = GLUT.BITMAP_8_BY_13;
//	private static int MARGIN = 6;
	private static double TICKS_PER_PIXEL = 1.0/60.0;

	
	public TickMarks(Canvas canvas) {
		_canvas = canvas;
	}
	
	public void draw(GL gl, Bounds bounds) {
		double w = _canvas.getWidth();
		double h = _canvas.getHeight();
		paintTicks(gl, w, h, bounds);
		paintLabels(gl, w, h, bounds);
	}

	public Bounds getBounds() {
		return new Bounds();
	}
	
	
	private static void drawString(String str) {
		glut.glutBitmapString(FONT, str); 
	}

	private static String tickToString(double tick, double length) {
		StringBuffer str;
		if (length > 0.00001 && length < 100000) {
			NumberFormat nf = new DecimalFormat("0.######");
			str = new StringBuffer(nf.format(tick));
		}
		else {
			NumberFormat nf = new DecimalFormat("0.######E0");
			str = new StringBuffer(nf.format(tick));
			int i = str.indexOf("E");
			str.replace(i, i+1, " E");
			//if (abs(tick) >= 1)
			//	str.insert(i+1, ' ');
			if (tick == 0)
				str = new StringBuffer("0");
		}
		
		if (tick >= 0)
			str.insert(0, ' ');
			
		return str.toString();
	}
	
	
	private static double log10(double x) {
		return log(x) / log(10);
	}
	
	private static Vector<Double> getTicks(double lo, double hi, double desiredTickNum) {
		// find "step" between ticks:  s = i * 10^p, where i is one of 1, 2, 5, 8,
		// and x is an integer.  desired number of ticks is about 10.
		
		double p1 = log10((hi-lo) / (1*desiredTickNum));
		double p2 = log10((hi-lo) / (2*desiredTickNum));
		double p5 = log10((hi-lo) / (5*desiredTickNum));
		// double p8 = log10((hi-lo) / 80);
		
		double d1 = abs(p1 - round(p1));
		double d2 = abs(p2 - round(p2));
		double d5 = abs(p5 - round(p5));
		// double d8 = abs(p8 - round(p8));
		
		double i = 1, p = p1, d = d1;
		if (d2 < d) { i = 2; p = p2; d = d2; }
		if (d5 < d) { i = 5; p = p5; d = d5; }
		// if (d8 < d) { i = 8; p = p8; d = d8; }
		p = round(p);
		
		double step = i * pow(10, p);		
		
		Vector<Double> ret = new Vector<Double>();
		double mult = floor(lo / step) + 1;
		while (mult*step < hi) {
			ret.add((Double)(mult*step));
			mult++;
		}
		return ret;
	}
	
	
	private static void paintTicks(GL gl, double w, double h, Bounds bounds) {
		gl.glColor3d(0.82f, 0.82f, 0.87f); // light gray
		gl.glBegin(GL.GL_LINES);
		
		for (double x : getTicks(bounds.xmin, bounds.xmax, w*TICKS_PER_PIXEL)) {
			gl.glVertex2d(x, bounds.ymin);
			gl.glVertex2d(x, bounds.ymax);
		}
		for (double y : getTicks(bounds.ymin, bounds.ymax, h*TICKS_PER_PIXEL)) {
			gl.glVertex2d(bounds.xmin, y);
			gl.glVertex2d(bounds.xmax, y);
		}
		
		gl.glEnd();
	}
	
	
	private static void paintLabels(GL gl, double w, double h, Bounds bounds) {
		gl.glColor3d(0, 0, 0);
		
		for (double x : getTicks(bounds.xmin, bounds.xmax, w*TICKS_PER_PIXEL)) {
			if ((x - bounds.xmin) / bounds.getWidth() < 0.05)
				continue;
			gl.glPushMatrix();
			gl.glRasterPos2d(x, bounds.ymin + bounds.getHeight()/64);
			drawString(tickToString(x, bounds.getWidth()));
			gl.glPopMatrix();
		}
		for (double y : getTicks(bounds.ymin, bounds.ymax, h*TICKS_PER_PIXEL)) {
			if ((y - bounds.ymin) / bounds.getHeight() < 0.05)
				continue;
			gl.glPushMatrix();
			gl.glRasterPos2d(bounds.xmin + bounds.getWidth()/64, y);
			drawString(tickToString(y, bounds.getHeight()));
			gl.glPopMatrix();
		}
	}
	
}
