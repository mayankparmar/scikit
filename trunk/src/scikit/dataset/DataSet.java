package scikit.dataset;

import static java.lang.Math.*;
import scikit.util.Bounds;


abstract public class DataSet {
	/** Returns data width: xmin, xmax, ymin, ymax */
	public Bounds getBounds() {
		Bounds ret = new Bounds();
		double[] data = copyData();
		for (int i = 0; i < data.length; i += 2) {
			ret.xmin = min(ret.xmin, data[i+0]);
			ret.xmax = max(ret.xmax, data[i+0]);
			ret.ymin = min(ret.ymin, data[i+1]);
			ret.ymax = max(ret.ymax, data[i+1]);
		}
		return ret;
	}
	
	/** Returns a copy of this dataset in the format [x1, y1, x2, y2, ...] */
	abstract public double[] copyData();
	
	/** Returns a copy of the subset of this data within range */
	// TODO pass bounds
	public double[] copyPartial(int N, double xmin, double xmax, double ymin, double ymax) {
		return copyData();
	}
}
