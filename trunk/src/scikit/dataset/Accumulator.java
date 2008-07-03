package scikit.dataset;

import java.util.*;

import static java.lang.Math.*;


public class Accumulator extends DataSet {
	private class Bin {
		double sum;
		double sum2;
		double count;
		public Bin() {
			sum = sum2 = 0;
			count = 0;
		}
		public void accum(Bin that) {
			sum += that.sum;
			sum2 += that.sum2;
			count += that.count;
		}
		public void accum(double value) {
			sum += value;
			sum2 += value*value;
			count += 1;
		}
		public double average() {
			return sum / count;
		}
		public double error() {
			double s1 = sum / count;
			double s2 = sum2 / count;
			return sqrt(s2-s1*s1) / sqrt(count);
		}
	}
	
	private double _binWidth;
	private SortedMap<Double, Bin> _hash;
    private boolean _errorBars = false;
    
	public Accumulator(double binWidth) {
		_hash = new TreeMap<Double, Bin>();
		_binWidth = binWidth;
	}

	public Accumulator() {
		this(0);
	}
	
	public Accumulator rebin(double binWidth) {
		Accumulator ret = new Accumulator(binWidth);
		for (Double k : keys()) {
			Bin v1 = ret._hash.get(k);
			Bin v2 = _hash.get(k);
			if (v1 == null) {
				v1 = new Bin();
				v1.accum(v2);
				ret._hash.put(ret.key(k), v1);
			}
			else {
				v1.accum(v2);
			}
		}
		return ret;
	}
	
	public void enableErrorBars(boolean errorBars) {
		_errorBars = true;
	}
	
	public void clear() {
		_hash = new TreeMap<Double, Bin>();
	}
	
	public DatasetBuffer copyData() {
		DatasetBuffer ret = new DatasetBuffer();
		ret._x = new double[_hash.size()];
		ret._y = new double[_hash.size()];
		if (_errorBars)
			ret._errY = new double[_hash.size()];
		int i = 0;
        for (Double k : _hash.keySet()) {
			ret._x[i] = k;
            ret._y[i] = eval(k);
            if (_errorBars)
            	ret._errY[i] = evalError(k);
            i++;
		}
		return ret;	
	}
	
	public Set<Double> keys() {
		return _hash.keySet();
	}
	
	public double eval(double x) {
		Bin val = _hash.get(key(x));
		return (val == null) ? Double.NaN : val.average();
	}
	
	public double evalError(double x) {
		Bin val = _hash.get(key(x));
		return (val == null) ? Double.NaN : val.error();		
	}
	
	public void accum(double x, double y) {
		Bin val = _hash.get(key(x));
		if (val == null) {
			val = new Bin();
			val.accum(y);
			_hash.put(key(x), val);
		}
		else {
			val.accum(y);
		}
	}
	
	// key() gives the unique hash for every bin. it is the double value representing
	// the center of the bin.
	private double key(double x) {
		if (_binWidth == 0)
			return x;
		else {
			double bw = _binWidth;
			double k = bw * rint(x/bw); 
			return k == -0 ? +0 : k;    // +-0 have different representations.  choose +0.
		}
	}
}
