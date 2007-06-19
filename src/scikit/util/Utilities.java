package scikit.util;

import static java.lang.Math.abs;
import java.text.DecimalFormat;
import scikit.jobs.Job;

public class Utilities {
	static DecimalFormat df1 = new DecimalFormat("0.####");
	static DecimalFormat df2 = new DecimalFormat("0.####E0");
	static public String format(double x) {
		return (abs(x) > 0.001 && abs(x) < 1000 || x == 0 ? df1 : df2).format(x);
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
