package scikit.dataset;

import static java.lang.Math.*;

abstract public class Function extends DataSet {
	double xmin, xmax, ymin, ymax;
	int N = 1024;
	
	public Function(double _xmin, double _xmax) {
		xmin = _xmin;
		xmax = _xmax;
        ymin = ymax = eval(xmin);
        for (double x = xmin; x < xmax; x += (xmax-xmin)/100) {
            ymin = min(ymin, eval(x));
            ymax = max(ymax, eval(x));
        }
	}
    
    public double[] getBounds() {
        return new double[] {xmin, xmax, ymin, ymax};
    }
    
	public double[] copyData() {
		return copyPartial(N, xmin, xmax, ymin, ymax);
	}
	
	public double[] copyPartial(int N, double xmin, double xmax, double ymin, double ymax) {
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
