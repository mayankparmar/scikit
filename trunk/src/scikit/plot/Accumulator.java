package scikit.plot;


import java.util.HashMap;
import static java.lang.Math.*;


public class Accumulator extends DataSet {
	private double _origBinWidth, _binWidth;
	private HashMap<Double, Double> _origHash, _hash;
	
	
	public Accumulator(double binWidth) {
		_origHash = new HashMap<Double, Double>();
		_hash = new HashMap<Double, Double>();
		_origBinWidth = _binWidth = binWidth;
	}
	
	
	public int size() {
		return _hash.size();
	}
	
	
	public double[] copyData() {
		return copyDataAux(_hash);
	}
	
	
	public void setBinWidth(double binWidth) {
		binWidth = max(binWidth, _origBinWidth);
		
		double[] data = copyDataAux(_origHash);
		_binWidth = binWidth;
		_hash = new HashMap<Double, Double>();
		for (int i = 0; i < data.length; i += 2) {
			accumAux(_hash, _binWidth, data[i], data[i+1]);
		}
	}
	
	public double getBinWidth() {
		return _binWidth;
	}
	
	public void accum(double x) {
		accum(x, 1.0);
	}
	
	public void accum(double x, double v) {
		accumAux(_origHash, _origBinWidth, x, v);
		accumAux(_hash, _binWidth, x, v);
	}
	
	private static void accumAux(HashMap<Double,Double> h, double bw, double x, double v) {
//		bw = bw * (1 - 1e-12); // perturb bin width slightly to get consistent rounding
		double k = bw * floor(x/bw + 0.5); // why doesn't rint(x/bw) work??
		Double obj = h.get(k);
		double vp = (obj == null) ? 0 : obj;
		h.put(k, vp + v);		
	}
	
	private static double[] copyDataAux(HashMap<Double, Double> h) {
		int i = 0;
		double[] ret = new double[2*h.size()];		
		for (Double k : h.keySet()) {
			ret[i++] = k;
			ret[i++] = h.get(k);
		}
		return ret;	
	}
}
