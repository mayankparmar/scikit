package kip.util;

import scikit.jobs.Callback;
import java.util.Vector;


public class Plot implements Callback {
	Gnuplot gnuplot = new Gnuplot();
	
	Vector<double[][]> data = new Vector();
	
	
	synchronized public void add(double[] x, double[] y) {
		data.add(new double[][]{x, y});
	}
	
	
	public void add(double xlo, double dx, double[] y) {
		double[] x = new double[y.length];
		for (int i = 0; i < y.length; i++)
			x[i] = xlo + i*dx;
		add(x, y);
	}
	
	
	synchronized public void setXRange(double xlo, double xhi) {
		gnuplot.cmd("set xrange ["+xlo+":"+xhi+"]");
	}
	
	
	synchronized public void setYRange(double ylo, double yhi) {
		gnuplot.cmd("set yrange ["+ylo+":"+yhi+"]");
	}
	
	
	synchronized public void setTitle(String title) {
		gnuplot.cmd("set title '" + title + "'");
	}
	
	synchronized public void setDataStyle(String s) {
		gnuplot.cmd("set data style " + s);
	}
	
	synchronized public void setLogScale(boolean x, boolean y) {
		String s;
		s = x ? "set" : "unset";
		gnuplot.cmd(s + " logscale x");
		s = y ? "set" : "unset";
		gnuplot.cmd(s + " logscale y");
	}
	
	
	synchronized public void repaint() {
		String s = "plot";
		for (int i = 0; i < data.size(); i++)
			s += " '-'";
		gnuplot.cmd(s);
		for (double[][] d : data)
			gnuplot.send(d[0], d[1]);
	}
	
	
	public void callback(Object o) {
		repaint();
	}
	
	
	public void destroy() {
		gnuplot.destroy();
	}
}
