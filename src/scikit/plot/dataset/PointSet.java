package scikit.dataset;


public class PointSet extends DataSet {
	private double[] _x, _y;
	
	
	public PointSet(double x0, double dx, double[] y) {
		_x = new double[y.length];
		for (int i = 0; i < y.length; i++) {
			_x[i] = x0 + i*dx;
		}
		_y = y;
	}
	
	
	public PointSet(double[] x, double[] y) {
		setXY(x, y);
	}
	
	
	public void setX(double[] x) {
		setXY(x, _y);
	}
	
	public void setY(double[] y) {
		setXY(_x, y);
	}
	
	public void setXY(double[] x, double[] y) {
		if (x.length != y.length)
			throw new IllegalArgumentException("Array sizes are not equal.");
		_x = x;
		_y = y;
	}
	
	
	public double[] copyData() {
		double[] ret = new double[2*_x.length];
		for (int i = 0; i < _x.length; i++) {
			ret[2*i+0] = _x[i];
			ret[2*i+1] = _y[i];
		}
		return ret;
	}
}
