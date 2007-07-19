package scikit.graphics;

import static java.lang.Math.*;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import scikit.util.Bounds;


public class TickMarks implements Drawable {
	private Plot _plot;
	
	private static double TICKS_PER_PIXEL = 1.0/60.0;
	// pixel length constants
	private static int MARGIN = 4;
	private static int LABEL_CUTOFF = 15;
	
	private static NumberFormat nf1 = new DecimalFormat("0.######");
	private static NumberFormat nf2 = new DecimalFormat("0.######E0");
	
	public TickMarks(Plot plot) {
		_plot = plot;
	}
	
	public void draw(Graphics g) {
		Bounds db = _plot.dataBounds();
		Bounds cb = _plot.pixelBounds();
		
		List<Tick> xticks, yticks;
		if (!_plot._logScaleX)
			xticks = getLinTicks(db.xmin, db.xmax, cb.getWidth()*TICKS_PER_PIXEL);
		else
			xticks = getLinTicks(db.xmin, db.xmax, cb.getWidth()*TICKS_PER_PIXEL);
		
		if (!_plot._logScaleY)
			yticks = getLinTicks(db.ymin, db.ymax, cb.getHeight()*TICKS_PER_PIXEL);
		else
			yticks = getLinTicks(db.ymin, db.ymax, cb.getHeight()*TICKS_PER_PIXEL);
		
		drawTickLines(g, xticks, yticks);
		drawTickLabels(g, xticks, yticks);
	}

	public Bounds getBounds() {
		return new Bounds();
	}
	
	
	private static String tickToString(double tick, double length) {
		StringBuffer str;
		if (abs(tick) > 0.00001 && abs(tick) < 100000) {
			str = new StringBuffer(nf1.format(tick));
		}
		else {
			str = new StringBuffer(nf2.format(tick));
			int i = str.indexOf("E");
			str.replace(i, i+1, "e");
			if (tick == 0)
				str = new StringBuffer("0");
		}
		
		// insert an empty space in place of negative sign
		if (tick >= 0)
			str.insert(0, ' ');
			
		return str.toString();
	}
	
	
	private static ArrayList<Tick> getLinTicks(double lo, double hi, double desiredTickNum) {
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
		
		ArrayList<Tick> ret = new ArrayList<Tick>();
		double mult = floor(lo / step) + 1;
		while (mult*step < hi) {
			ret.add(new Tick(mult*step, tickToString(mult*step, hi-lo), new Color(0.82f, 0.82f, 0.87f)));
			mult++;
		}
		return ret;
	}
	
	private void drawTickLines(Graphics g, List<Tick> xticks, List<Tick> yticks) {
		Bounds db = _plot.dataBounds();
		for (Tick tick : xticks) {
			g.setColor(tick.color);
			g.drawLine(tick.v, db.ymin, tick.v, db.ymax);
		}
		for (Tick tick : yticks) {
			g.setColor(tick.color);
			g.drawLine(db.xmin, tick.v, db.xmax, tick.v);
		}
	}
	
	private void drawTickLabels(Graphics g, List<Tick> xticks, List<Tick> yticks) {
		Bounds db = _plot.dataBounds();
		Bounds cb = _plot.pixelBounds();
		double heightPerPix = db.getHeight() / cb.getHeight();
		double widthPerPix = db.getWidth() / cb.getWidth();
		
		g.setColor(Color.BLACK);
		for (Tick tick : xticks) {
			double boundaryDistance = min(tick.v-db.xmin, db.xmax-tick.v) / widthPerPix;
			if (tick.str != null && boundaryDistance > LABEL_CUTOFF) {
				double y = db.ymin + MARGIN*heightPerPix;
				g.drawString(tick.str, tick.v-g.stringWidth(" "+tick.str)/2, y);
			}
		}
		for (Tick tick : yticks) {
			if (tick.str != null) {
				double x = db.xmin + MARGIN*widthPerPix;
				g.drawString(tick.str, x, tick.v-g.stringHeight(tick.str)/2);
			}
		}
	}
}


class Tick {
	double v;
	String str;
	Color color;
	public Tick(double v, String str, Color color) {
		this.v = v;
		this.str = str;
		this.color = color;
	}
}
