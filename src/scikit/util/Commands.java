package scikit.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;

import scikit.dataset.DataSet;
import scikit.dataset.PointSet;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import bsh.ClassPathException;
import bsh.util.ClassBrowser;

public class Commands {
	private static Plot _lastPlot = null;
	private static JFrame _lastPlotFrame = null;
	private static int _plotCnt = 0;
	private static Color[] _plotColors = new Color[] {Color.BLUE, Color.RED, Color.BLACK, Color.GREEN, Color.CYAN, Color.PINK}; 
	
	public static void plot(DataSet data, String name) {
		Plot plot = new Plot("Plot");
		plot.registerPoints(name, data, _plotColors[0]);
		_lastPlotFrame = Utilities.frame(plot.getComponent(), plot.getTitle());
		_lastPlot = plot;
		_plotCnt = 1;
	}
	public static void plot(DataSet data) {
		plot(data, "data");
	}
	public static void replot(DataSet data, String name) {
		if (_lastPlot == null) {
			plot(data, name);
		}
		else {
			Color c = _plotColors[_plotCnt % _plotColors.length];
			_lastPlot.registerPoints(name, data, c);
			_lastPlotFrame.setVisible(true);
			_plotCnt++;
		}
	}
	public static void replot(DataSet data) {
		replot(data, "data"+_plotCnt);
	}
	public static void plot(double[] data) {
		plot(new PointSet(0, 1, DoubleArray.clone(data)));
	}
	public static void replot(double[] data) {
		replot(new PointSet(0, 1, DoubleArray.clone(data)));
	}

	public static void grid(int w, int h, double[] data) {
		Grid grid = new Grid("Grid");
		grid.registerData(w, h, data);
		Utilities.frame(grid);
	}
	
	public static void grid3d() {
		Utilities.frame(new Grid3D("Grid"));
	}
	
	public static void grid3d(int w, int h, int d, double[] data) {
		Grid3D grid = new Grid3D("Grid");
		grid.registerData(w, h, d, data);
		Utilities.frame(grid);
	}
	
	public static void grid3d(Array3d a) {
		grid3d(a.nx(), a.ny(), a.nz(), a.array());
	}
	
	public static void classBrowser() { 
		ClassBrowser browser = new ClassBrowser();
		try {
			browser.init();
		} catch(ClassPathException e) {}
		Utilities.frame(browser, "Class browser");
	}
	
	public static Array3d load3d() {
		try {
			String fname = FileUtil.loadDialog(new JDialog(), "");
			if (fname != null)
				return new Array3d(new File(fname));
		} catch(IOException e) {}
		return null;
	}
	
	public static void save(BufferedImage image, String fname) {
		try {
			if (fname != null) {
				int offset = fname.lastIndexOf( "." );
				String type;
				if (offset == -1) {
					fname += ".png";
					type = "png";
				}
				else {
					type = fname.substring(offset + 1);
				}
				ImageIO.write(image, type, new File(fname));
			}
		} catch(IOException e) {}
	}
	
	public static void save(BufferedImage image) {
		try {
			save(image, FileUtil.saveDialog(new JDialog(), ""));
		} catch(IOException e) {} 
	}
}
