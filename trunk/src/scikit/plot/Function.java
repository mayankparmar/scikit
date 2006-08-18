package scikit.plot;

import static java.lang.Math.*;

abstract public class Function extends DataSet {
	double xmin, xmax;
	int N = 100;
	
	public Function(double _xmin, double _xmax) {
		xmin = _xmin;
		xmax = _xmax;
	}
	
	public double[] copyData() {
		return copyPartial(N, xmin, xmax, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	
	public double[] copyPartial(int N, double xmin, double xmax, double ymin, double ymax) {
		xmin = max(xmin, this.xmin);
		xmax = min(xmax, this.xmax);
		
		double[] ret = new double[2*N];
		for (int i = 0; i < N; i++) {
			double x = (xmax - xmin) * i / (N-1) + xmin;
			ret[2*i] = x;
			ret[2*i+1] = eval(x);
		}
		return ret;
	}
	
	public abstract double eval(double x);
}
