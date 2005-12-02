package kip.util;


import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.IOException;


public class Gnuplot {
	Process p;
	PrintWriter pw;
	
	public Gnuplot() {
		try {
			p = Runtime.getRuntime().exec("/sw/bin/gnuplot");
			OutputStream out = p.getOutputStream();
			pw = new PrintWriter(out);
			// pw.println("set term x11");
			pw.println("plot sin(x)");
			pw.flush();
		} catch (IOException e) {
			System.err.println("Gnuplot not available.");
		}
	}
	
	
	public void cmd(String s) {
		pw.println(s);
		pw.flush();
	}
	
	
	public void send(double[] y) {
		for (int i = 0; i < y.length; i++) {
			pw.println(y[i]);
		}
		pw.println("e");
		pw.flush();
	}
	
	
	public void send(double[] x, double[] y) {
		for (int i = 0; i < x.length; i++) {
			pw.println(x[i] + " " + y[i]);
		}
		pw.println("e");
		pw.flush();
	}
	
	
	public void plot(double[] y) {
		cmd("plot '-'");
		send(y);
	}
	
	
	public void plot(double[] x, double[] y) {
		cmd("plot '-'");
		send(x, y);
	}
	
	
	public void destroy() {
		if (p != null) {
			p.destroy();
		}
	}
	
	protected void finalize() {
		destroy();
	}
}
