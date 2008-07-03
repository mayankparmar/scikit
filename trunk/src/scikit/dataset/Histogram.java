package scikit.dataset;


import java.util.*;

import static java.lang.Math.*;


public class Histogram extends DataSet {
	private double _binWidth;
    // maps keys to the total accumulated value per bin
	private AbstractMap<Double, Double> _hash;    
    private double _fullSum = 0;
	private boolean _norm = false;
	
	public Histogram(double binWidth) {
		_hash = new TreeMap<Double, Double>();
		_binWidth = binWidth;
	}
	
	public Histogram rebin(double binWidth) {
		Histogram ret = new Histogram(binWidth);
		ret._fullSum = _fullSum;
		ret._norm = _norm;
		for (Double k : keys()) {
			Double y = ret._hash.get(k);
			if (y == null)
				y = 0.0;
			ret._hash.put(ret.key(k), _hash.get(k)+y);
		}
		return ret;
	}
	
	public void clear() {
		_hash = new TreeMap<Double, Double>();
	}
    
    public void setNormalizing(boolean norm) {
        _norm = norm;
    }

	public DatasetBuffer copyData() {
		DatasetBuffer ret = new DatasetBuffer();
		ret._x = new double[_hash.size()];
		ret._y = new double[_hash.size()];
		int i = 0;
        for (Double k : _hash.keySet()) {
			ret._x[i] = k;
            ret._y[i] = eval(k);
            i++;
		}
		return ret;	
	}
	
	public Set<Double> keys() {
		return _hash.keySet();
	}
	
	public double eval(double x) {
		Double y = _hash.get(key(x));
		if (y == null) {
			return Double.NaN;
		}
		else if (_norm)
            return y / (_binWidth * _fullSum);
        else
        	return y;
	}
	
	public void accum(double x, double y) {
		Double yp = _hash.get(key(x));
		if (yp == null)
			yp = 0.0;
		_hash.put(key(x), y+yp);
		_fullSum += y;
	}
	
	public void accum(double x) {
		accum(x, 1.0);
	}
	
	private double key(double x) {
		double bw = _binWidth;
		double k = bw * rint(x/bw); // each binning cell is labeled by its center coordinate, key().
		return k == -0 ? +0 : k;    // +-0 have different representations.  choose +0. 
	}
}
