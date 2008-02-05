package scikit.dataset;

import java.util.*;

import static java.lang.Math.*;


public class Accumulator extends DataSet {
	private double _binWidth;
    // maps keys to an array of two numbers. the first is
    // total accumulated value for this bin. the second is a
    // count of the accumulation operations performed.
	private AbstractMap<Double, double[]> _hash;
    
	public Accumulator(double binWidth) {
		_hash = new TreeMap<Double, double[]>();
		_binWidth = binWidth;
	}
	
	public Accumulator rebin(double binWidth) {
		Accumulator ret = new Accumulator(binWidth);
		for (Double k : keys()) {
			double[] v1 = ret._hash.get(k);
			double[] v2 = _hash.get(k);
			double[] v = (v1 == null) ?
						new double[]{v2[0], v2[1]} :
						new double[]{v1[0]+v2[0], v1[1]+v2[1]};
			ret._hash.put(ret.key(k), v);
		}
		return ret;
	}
	
	public void clear() {
		_hash = new TreeMap<Double, double[]>();
	}
	
	public double[] copyData() {
		int i = 0;
		double[] ret = new double[2*_hash.size()];
        for (Double k : _hash.keySet()) {
			ret[i] = k;
            ret[i+1] = eval(k);
            i += 2;
		}
		return ret;	
	}
	
	public Set<Double> keys() {
		return _hash.keySet();
	}
	
	public double eval(double x) {
		double[] val = _hash.get(key(x));
		return (val == null) ? Double.NaN : val[0]/val[1];
	}
	
	public void accum(double x, double y) {
		double[] val = _hash.get(key(x));
		if (val == null)
			val = new double[] {y, 1};
		else {
			val[0] += y;
			val[1] += 1;
		}
		_hash.put(key(x), val);
	}
	
	private double key(double x) {
		double bw = _binWidth;
		double k = bw * rint(x/bw); // each binning cell is labeled by its center coordinate, key().
		return k == -0 ? +0 : k;    // +-0 have different representations.  choose +0. 
	}
}
