package scikit.dataset;


abstract public class DiscreteFunction extends DataSet {
	double x[];
	int stride;
	
	public DiscreteFunction(double[] x, int stride) {
		this.x = x;
		this.stride = stride;
	}
    
	public double[] copyData() {
		return copyPartial(-1, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.NaN);
	}
	
	public double[] copyPartial(int N, double xmin, double xmax, double ymin, double ymax) {
		int npts = 0;
		for (int i = 0; i < x.length/stride; i++) {
			if (x[stride*i] > xmin && x[stride*i] < xmax)
				npts++;
		}
		
		int cnt = 0;
		double[] ret = new double[2*npts];
		for (int i = 0; i*stride < x.length; i++) {
			if (x[stride*i] > xmin && x[stride*i] < xmax) {
				ret[cnt++] = x[stride*i];
				ret[cnt++] = eval(x[stride*i]);
			}
		}
		return ret;
	}
	
	public abstract double eval(double x);
}
