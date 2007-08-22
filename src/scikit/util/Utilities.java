package scikit.util;

import static java.lang.Math.abs;

import java.awt.Color;
import java.io.DataInput;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import scikit.graphics.dim2.Plot;


public class Utilities {
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
	
	public static double readDoubleLittleEndian(DataInput dis) throws IOException {
		long accum = 0;
		for (int shiftBy=0; shiftBy<64; shiftBy+=8) {
			// must cast to long or shift done modulo 32
			accum |= ((long)(dis.readByte() & 0xff)) << shiftBy;
		}
		return Double.longBitsToDouble(accum);
	}
	
	public static float readFloatLittleEndian(DataInput dis) throws IOException {
		int accum = 0;
		for (int shiftBy=0; shiftBy<32; shiftBy+=8) {
			// must cast to long or shift done modulo 32
			accum |= (dis.readByte() & 0xff ) << shiftBy;
		}
		return Float.intBitsToFloat(accum);
	}
	
	public static int readIntLittleEndian(DataInput dis) throws IOException {
		return Integer.reverseBytes(dis.readInt());
	}
	
	public static long readLongLittleEndian(DataInput dis) throws IOException {
		return Long.reverseBytes(dis.readLong());
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
	
	
	/*
	public static int[] toArray(Collection<Integer> c) {
		Integer a[] = c.toArray(new Integer[0]);
		int[] ret = new int[a.length];
		for (int i = 0; i < a.length; i++)
			ret[i] = a[i];
		return ret;
	}
	*/
	
	// utility method for quickly viewing data.
	private static Plot debugPlot;
	public static void plot(double[] data) {
		if (debugPlot == null) {
			debugPlot = new Plot("Debug plot");
		}
		debugPlot.registerPoints("", new scikit.dataset.PointSet(0, 1, data), Color.BLACK);
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
