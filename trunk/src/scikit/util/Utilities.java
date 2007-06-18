package scikit.util;

import scikit.jobs.Job;

public class Utilities {
	
	
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
