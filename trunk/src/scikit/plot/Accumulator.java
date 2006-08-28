package scikit.plot;


import java.util.HashMap;
import static java.lang.Math.*;


public class Accumulator extends DataSet {
	private double _origBinWidth, _binWidth;
	private HashMap<Double, double[]> _origHash, _hash;
	private boolean _avg = false;
	
	public Accumulator(double binWidth) {
		_origHash = new HashMap<Double, double[]>();
		_hash = new HashMap<Double, double[]>();
		_origBinWidth = _binWidth = binWidth;
	}
	
	
	public double[] copyData() {
		int i = 0;
		double[] ret = new double[2*_hash.size()];		
		for (Double k : _hash.keySet()) {
			ret[i++] = k;
			ret[i++] = _hash.get(k)[0] / (_avg == true ? _hash.get(k)[1] : 1);
		}
		return ret;	
	}
	
	
	public void setBinWidth(double binWidth) {
		_binWidth = max(binWidth, _origBinWidth);
		_hash = new HashMap<Double, double[]>();
		for (Double k : _origHash.keySet()) {
			accumAux(_hash, _binWidth, k, _origHash.get(k)[0], _origHash.get(k)[1]);
		}
	}
	
	public double getBinWidth() {
		return _binWidth;
	}
	
	public void setAveraging(boolean avg) {
		_avg = avg;
	}
	
	public void accum(double x) {
		accum(x, 1.0);
	}
	
	public void accum(double x, double v) {
		accumAux(_origHash, _origBinWidth, x, v, 1);
		accumAux(_hash, _binWidth, x, v, 1);
	}
	
	private static void accumAux(HashMap<Double,double[]> h, double bw, double x, double v, double cnt) {
//		bw = bw * (1 - 1e-12); // perturb bin width slightly to get consistent rounding
		double k = bw * floor(x/bw + 0.5); // why doesn't rint(x/bw) work??
		double[] val = h.get(k);
		if (val == null)
			val = new double[] {0, 0};
		val[0] += v;
		val[1] += cnt;
		h.put(k, val);
	}
}
