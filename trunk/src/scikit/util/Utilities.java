package scikit.util;

import static java.lang.Math.abs;
import java.text.DecimalFormat;
import java.util.Collection;

import scikit.jobs.Job;

public class Utilities {
	static DecimalFormat df1 = new DecimalFormat("0.####");
	static DecimalFormat df2 = new DecimalFormat("0.####E0");
	static public String format(double x) {
		return (abs(x) > 0.001 && abs(x) < 1000 || x == 0 ? df1 : df2).format(x);
	}
	
	
	public static double periodicOffset(double L, double dx) {
		if (dx >  L/2) return L-dx;
		if (dx < -L/2) return L+dx;
		return dx;
	}
	
	public static int[] integerSequence(int n) {
		int ret[] = new int[n];
		for (int i = 0; i < n; i++)
			ret[i] = i;
		return ret;
	}
	
	public static int sumArray(int[] n) {
		int ret = 0;
		for (int i = 0; i < n.length; i++)
			ret += n[i];
		return ret;
	}
	
	public static int[] toArray(Collection<Integer> c) {
		Integer a[] = c.toArray(new Integer[0]);
		int[] ret = new int[a.length];
		for (int i = 0; i < a.length; i++)
			ret[i] = a[i];
		return ret;
	}
	
	// utility method for quickly viewing data.
	private static scikit.plot.Plot debugPlot;
	public static void plot(int i, double[] data) {
		if (debugPlot == null) {
			debugPlot = new scikit.plot.Plot("Debug plot", true);
			Job.addDisplay(debugPlot);
		}
		debugPlot.setDataSet(i, new scikit.dataset.PointSet(0, 1, data));
		// try to force an immediate repaint
		debugPlot.animate();
		Thread.yield();
	}

	
	
	static int _frameStagger = 100;	
	public static javax.swing.JFrame frame(java.awt.Component comp, String title) {
		javax.swing.JFrame frame = new javax.swing.JFrame(title);
		frame.getContentPane().add(comp);
		frame.setLocation(_frameStagger, _frameStagger);		
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		_frameStagger += 60;		
		return frame;
	}

}
