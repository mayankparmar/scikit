package kip.util;

public class DoubleArray {
	public static double min(double a[]) {
		double min = a[0];
		for (double v : a)
			if (v < min) min = v;
		return min;
	}
	
	public static double max(double a[]) {
		double max = a[0];
		for (double v : a)
			if (v > max) max = v;
		return max;
	}
	
	public static double mean(double a[]) {
		double sum = 0;
		for (double v : a)
			sum += v;
		return sum/a.length;
	}
	
	public static double meanSquared(double a[]) {
		double sum = 0;
		for (double v : a)
			sum += v*v;
		return sum/a.length;
	}
}
