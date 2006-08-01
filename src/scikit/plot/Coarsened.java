package scikit.plot;

import static java.lang.Math.*;

public class Coarsened extends DataSet {
	private double[] _y, _cgy;
	private double _xlo, _xstep, _cgstep;
	
	public Coarsened(double[] y, double xlo, double xhi) {
		_y = _cgy = y;
		_xlo = xlo;
		_xstep = _cgstep = (xhi-xlo)/(y.length-1);
	}
	
	public Coarsened(double[] y) {
		this(y, 0, y.length-1);
	}
	
	public int size() {
		return 2*_cgy.length;
	}
	
	public void setBinWidth(double cgstep) {
		double xlen = (_y.length-1)*_xstep;
		int n = (int)min(_y.length, round(xlen / cgstep) + 1);
		if (n != _cgy.length) {
			_cgstep = xlen / (n-1);
			_cgy = new double[n];
			updateAll();
		}
	}
	
	public void updateAll() {
		long N = _y.length; // long type prevents overflow of intermediate operations
		int n = _cgy.length;
		for (int j = 0; j < n; j++) {
			int i1 = (int)(j*N/n);
			int i2 = (int)((j+1)*N/n);
			double acc = 0;
			for (int i = i1; i < i2; i++)
				acc += _y[i];
			_cgy[j] = acc / (i2-i1);
		}
	}

	public double[] copyData() {
		double[] ret = new double[2*_cgy.length];
		for (int j = 0; j < _cgy.length; j++) {
			ret[2*j+0] = _xlo + (j+0.5)*_cgstep;
			ret[2*j+1] = _cgy[j];
		}
		return ret;
	}
}
