package scikit.dataset;


import java.util.*;
import static java.lang.Math.*;


public class Accumulator extends DataSet {
	// two accumulators are used internally. the "orig" accumulator
	// may use a possibly smaller binwidth that the regular accumulator.
	// by storing the "orig" accumulator, no information is lost when
	// the binwidth is increased.
	private double _origBinWidth, _binWidth;
    // each bin holds an array of two numbers.  the first is
    // total accumulated value for this bin.  the second is a
    // count of the accumulation operations performed, in case
    // averaging is desired.
	private AbstractMap<Double, double[]> _origHash, _hash;
    private double _fullSum = 0;
	private boolean _avg = false;
	private boolean _norm = false;
    
	public Accumulator(double binWidth) {
		_origHash = new TreeMap<Double, double[]>();
		_hash = new TreeMap<Double, double[]>();
		_origBinWidth = _binWidth = binWidth;
	}
	
	public void clear() {
		_origHash = new TreeMap<Double, double[]>();
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
	
	public void setBinWidth(double binWidth) {
		if (binWidth != _binWidth) {
			_binWidth = max(binWidth, _origBinWidth);
			_hash = new TreeMap<Double, double[]>();
			for (Double k : _origHash.keySet()) {
				accumAux(_hash, _binWidth, k, _origHash.get(k)[0], _origHash.get(k)[1]);
			}
		}
	}
	
	public double getBinWidth() {
		return _binWidth;
	}
	
	public void setAveraging(boolean avg) {
		_avg = avg;
	}
    
    public void setNormalizing(boolean norm) {
        _norm = norm;
    }
	
	public double eval(double x) {
		double[] val = _hash.get(key(x, _binWidth));
		if (val == null)
			return Double.NaN;
        if (_norm)
            return val[0] / (_binWidth * _fullSum);
        else if (_avg)
            return val[0] / val[1];
        else
        	return val[0];
	}

	public void accum(double x) {
		accum(x, 1.0);
	}
	
	public void accum(double x, double v) {
		accumAux(_origHash, _origBinWidth, x, v, 1);
		accumAux(_hash, _binWidth, x, v, 1);
        _fullSum += v;
	}
	
	private static double key(double x, double bw) {
		return bw * rint(x/bw); // each binning cell is labeled by its center coordinate, key().
	}
	
	private static void accumAux(AbstractMap<Double,double[]> h, double bw, double x, double v, double cnt) {
		double[] val = h.get(key(x, bw));
		if (val == null)
			val = new double[] {0, 0};
		val[0] += v;
		val[1] += cnt;
		h.put(key(x, bw), val);
	}
}
