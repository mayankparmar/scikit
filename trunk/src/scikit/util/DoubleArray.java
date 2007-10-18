package scikit.util;


public class DoubleArray {
	public static void copy(double src[], double dst[]) {
		if (src.length != dst.length)
			throw new IllegalArgumentException("Array lengths don't match.");
		for (int i = 0; i < src.length; i++)
			dst[i] = src[i];
	}
	
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
	
	public static void shift(double a[], double b) {
		for (int i = 0; i < a.length; i++)
			a[i] += b;
	}
	
	public static void scale(double a[], double b) {
		for (int i = 0; i < a.length; i++)
			a[i] *= b;		
	}
	
	public static double sum(double a[]) {
		double sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += a[i];
		return sum;
	}
	
	public static double mean(double a[]) {
		return sum(a)/a.length;
	}
	
	public static double meanSquared(double a[]) {
		return dot(a,a)/a.length;
	}
	
	public static double variance(double a[]) {
		double m = mean(a);
		return meanSquared(a) - m*m;
	}

	public static double dot(double a[], double b[]) {
		if (a.length != b.length)
			throw new IllegalArgumentException("Array lengths don't match.");
		double sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += a[i]*b[i];
		return sum;
	}
}
