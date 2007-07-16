package scikit.graphics;

import static java.lang.Math.*;
import java.awt.Canvas;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import scikit.util.Bounds;


public class TickMarks implements Drawable {
	private Canvas _canvas;
	
	private static double TICKS_PER_PIXEL = 1.0/60.0;
	// pixel length constants
	private static int MARGIN = 4;
	private static int LABEL_CUTOFF = 15;
	
	private static NumberFormat nf1 = new DecimalFormat("0.######");
	private static NumberFormat nf2 = new DecimalFormat("0.######E0");

	
	public TickMarks(Canvas canvas) {
		_canvas = canvas;
	}
	
	public void draw(Graphics g) {
		double w = _canvas.getWidth();
		double h = _canvas.getHeight();
		paintTicks(g, w, h, g.scene().dataBounds());
		paintLabels(g, w, h, g.scene().dataBounds());
	}

	public Bounds getBounds() {
		return new Bounds();
	}
	
	
	private static String tickToString(double tick, double length) {
		StringBuffer str;
		if (length > 0.00001 && length < 100000) {
			str = new StringBuffer(nf1.format(tick));
		}
		else {
			str = new StringBuffer(nf2.format(tick));
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
	
	
	private static void paintTicks(Graphics g, double w, double h, Bounds bounds) {
		g.setColor(new Color(0.82f, 0.82f, 0.87f)); // light gray
		for (double x : getTicks(bounds.xmin, bounds.xmax, w*TICKS_PER_PIXEL)) {
			g.drawLine(x, bounds.ymin, x, bounds.ymax);
		}
		for (double y : getTicks(bounds.ymin, bounds.ymax, h*TICKS_PER_PIXEL)) {
			g.drawLine(bounds.xmin, y, bounds.xmax, y);
		}
	}
	
	
	private static void paintLabels(Graphics g, double pixWidth, double pixHeight, Bounds bounds) {
		double widthPerPix = bounds.getWidth() / pixWidth;
		double heightPerPix = bounds.getHeight() / pixHeight;
		
		g.setColor(Color.BLACK);
		
		for (double x : getTicks(bounds.xmin, bounds.xmax, pixWidth*TICKS_PER_PIXEL)) {
			if (min(x-bounds.xmin, bounds.xmax-x) < LABEL_CUTOFF*widthPerPix)
				continue;
			String label = tickToString(x, bounds.getWidth());
			double y = bounds.ymin + MARGIN*heightPerPix;
			g.drawString(label, x-g.stringWidth(" "+label)/2, y);
		}
		for (double y : getTicks(bounds.ymin, bounds.ymax, pixHeight*TICKS_PER_PIXEL)) {
			if (min(y-bounds.ymin, bounds.ymax-y) < LABEL_CUTOFF*heightPerPix)
				continue;
			String label = tickToString(y, bounds.getHeight());
			double x = bounds.xmin + MARGIN*widthPerPix;
			g.drawString(label, x, y-g.stringHeight(label)/2);
		}
	}
}
