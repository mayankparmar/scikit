package scikit.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import scikit.graphics.dim2.Plot;

import static java.lang.Math.*;


public class Utilities {
	public final static int OPTIMAL_FRAME_SIZE = 300;

	static DecimalFormat df1 = new DecimalFormat("0.#######");
	static DecimalFormat df2 = new DecimalFormat("0.#######E0");
	static public String format(double x) {
		return (abs(x) > 0.0001 && abs(x) < 10000 || x == 0 ? df1 : df2).format(x);
	}
	
	public static double periodicOffset(double L, double dx) {
		if (dx >  L/2) return dx-L;
		if (dx < -L/2) return dx+L;
		return dx;
	}
	
	public static int[] integerSequence(int n) {
		int ret[] = new int[n];
		for (int i = 0; i < n; i++)
			ret[i] = i;
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(T o1) {
		return Arrays.asList(o1);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(T o1, T o2) {
		return Arrays.asList(o1, o2);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(T o1, T o2, T o3) {
		return Arrays.asList(o1, o2, o3);
	}
		
	// utility method for quickly viewing data.
	private static Plot debugPlot;
	public static void plot(double[] data) {
		if (debugPlot == null) {
			debugPlot = new Plot("Debug plot");
		}
		debugPlot.registerPoints("", new scikit.dataset.PointSet(0, 1, data), Color.BLACK);
		Thread.yield();
	}
	
	
	private static int _frameStagger = 100;
	public static JFrame frame(Component comp, String title) {
		JFrame frame = new JFrame(title);
		frame.getContentPane().add(comp);
		frame.setLocation(_frameStagger, _frameStagger);		
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		_frameStagger += 60;		
		return frame;
	}
	
	public static void frame(Frameable... fs) {
		for (Frameable f : fs)
			frame(f.getComponent(), f.getTitle());
	}
	
	public static JFrame frameTogether(String title, Frameable... fs) {
		int n = fs.length;
		int cols = (int)ceil(sqrt(n));
		int rows = (int)ceil((double)n/cols);
		int hgap = 2, vgap = 2;
		JPanel panel = new JPanel(new GridLayout(rows, cols, hgap, vgap));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		for (Frameable f : fs) {
			JPanel item = new JPanel(new BorderLayout());
			item.setBorder(BorderFactory.createTitledBorder(f.getTitle()));
			item.add(f.getComponent());
			panel.add(item);
		}
		// adjust panel's preferred size to be closer to OPTIMAL_FRAME_SIZE
		Dimension d = panel.getPreferredSize();
		double w = d.getWidth(), h = d.getHeight();
		double opt = OPTIMAL_FRAME_SIZE;
		if (max(w, h) > opt) {
			double scale = (0.4*(max(w,h) - opt) + opt) / max(w,h);
			panel.setPreferredSize(new Dimension((int)(w*scale), (int)(h*scale)));
		}
		return frame(panel, title);
	}

}
