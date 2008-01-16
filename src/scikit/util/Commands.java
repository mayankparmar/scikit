package scikit.util;

import static java.lang.Math.PI;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;

import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import scikit.numerics.fft.util.FFT3D;
import bsh.ClassPathException;
import bsh.util.ClassBrowser;

public class Commands {
	
	// utility method for quickly viewing data.
	public static void plot(double[] data) {
		Plot plot = new Plot("Quick Plot");
		plot.registerPoints("", new PointSet(0, 1, DoubleArray.clone(data)), Color.BLUE);
		Utilities.frame(plot);
	}
	
	public static void grid3d(int w, int h, int d, double[] data) {
		Grid3D grid = new Grid3D("Quick Grid");
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
	
	public static Array3d fftReal(Array3d a) {		
		FFT3D fft = new FFT3D(a.nx(), a.ny(), a.nz());
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
