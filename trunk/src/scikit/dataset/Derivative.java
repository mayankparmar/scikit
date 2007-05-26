package scikit.dataset;

public class Derivative extends DataSet {
	private DataSet _src;
	
	public boolean invertDependentParameter = false;
	
	public Derivative(DataSet src) {
		_src = src;
	}
	
	public double[] copyData() {
		double[] dat = _src.copyData();
		double[] ret = new double[dat.length-2];
		
		for (int i = 0; i < ret.length; i += 2) {
			double x1 = dat[i+0];
			double y1 = dat[i+1];
			double x2 = dat[i+2];
			double y2 = dat[i+3];
			ret[i+0] = (invertDependentParameter ? (y1+y2) : (x1+x2)) / 2;
			ret[i+1] = (y2 - y1) / (x2 - x1);
		}
		return ret;
	}

}
