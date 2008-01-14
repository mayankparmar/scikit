package scikit.util;

import java.awt.Color;

import bsh.ClassPathException;
import bsh.util.ClassBrowser;

import scikit.graphics.dim2.Plot;

public class Commands {
	
	// utility method for quickly viewing data.
	public static void plot(double[] data) {
		Plot plot = new Plot("Debug plot");
		plot.registerPoints("", new scikit.dataset.PointSet(0, 1, data), Color.BLUE);
		Utilities.frame(plot);
//		Thread.yield();
	}
	
	public static void classBrowser() { 
		ClassBrowser browser = new ClassBrowser();
		try {
			browser.init();
		} catch(ClassPathException e) {}
		Utilities.frame(browser, "Class browser");
	}
}
