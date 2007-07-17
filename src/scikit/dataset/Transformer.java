package scikit.dataset;

import scikit.util.Point;

abstract public class Transformer extends DataSet {
	DataSet _orig;
	
	public Transformer(DataSet orig) {
		_orig = orig;
	}

	public double[] copyData() {
		double[] ret = _orig.copyData();
		Point pt = new Point();
		for (int i = 0; i < ret.length; i += 2) {
			pt.x = ret[i+0];
			pt.y = ret[i+1];
			transform(pt);
			ret[i+0] = pt.x;
			ret[i+1] = pt.y;
		}
		return ret;
	}
	
	abstract public void transform(Point pt);
}
