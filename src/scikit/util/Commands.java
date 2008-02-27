package scikit.util;

import static java.lang.Math.PI;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFrame;

import scikit.dataset.DataSet;
import scikit.dataset.PointSet;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import scikit.numerics.fft.FFT3D;
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
	
	public static void grid(int w, int h, int d, double[] data) {
		Grid3D grid = new Grid3D("Grid");
		grid.registerData(w, h, d, data);
		Utilities.frame(grid);
	}
	
	public static void grid(Array3d a) {
		grid(a.nx(), a.ny(), a.nz(), a.array());
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
	
	public static Array3d fftReal(Array3d a) {		
		FFT3D fft = FFT3D.create(a.nx(), a.ny(), a.nz());
		fft.setLengths(a.lx(), a.ly(), a.lz());
		fft.transform(a.array(), fft.getScratch());

		Array3d ret = new Array3d(a.nx(), a.ny(), a.nz());
		double dx = a.lx()/a.nx();
		double dy = a.ly()/a.ny();
		double dz = a.lz()/a.nz();
		ret.setLengths(2*PI/dx, 2*PI/dy, 2*PI/dz);
		for (int i = 0; i < ret.array().length; i++)
			ret.array()[i] = fft.getScratch()[2*i+0];
		return ret;
	}
}