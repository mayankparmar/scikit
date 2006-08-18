package scikit.plot;

import static java.lang.Math.*;


abstract public class DataSet {
	/** Returns data width: xmin, xmax, ymin, ymax */
	public double[] getBounds() {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double[] data = copyData();
		for (int i = 0; i < data.length; i += 2) {
			minX = min(minX, data[i+0]);
			maxX = max(maxX, data[i+0]);
			minY = min(minY, data[i+1]);
			maxY = max(maxY, data[i+1]);
		}
		return new double[] {minX, maxX, minY, maxY};
	}
	
	/** Returns a copy of this dataset */
	abstract public double[] copyData();
	
	/** Returns a copy of the subset of this data within range */
	public double[] copyPartial(int N, double xmin, double xmax, double ymin, double ymax) {
		return copyData();
	}
}
