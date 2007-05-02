package scikit.dataset;

import static java.lang.Math.*;

public class YArray extends DataSet {
	private double[] _y;
	private double _xlo, _xstep;

	public YArray(double[] y, double xlo, double xhi) {
		_y = y;
		_xlo = xlo;
		_xstep = (xhi-xlo)/(y.length-1);
	}
	
	public YArray(double[] y) {
		this(y, 0, y.length-1);
	}
	
	public double[] copyData() {
		double[] ret = new double[2*_y.length];
		for (int j = 0; j < _y.length; j++) {
			ret[2*j+0] = _xlo + j*_xstep;
			ret[2*j+1] = _y[j];
		}
		return ret;
	}
	
	/*
	public double[] copyData() {
		double[] ret = new double[2*_nOut];
		for (int j = 0; j < _nOut; j++) {
			int i1 = j*_y.length/_nOut;
			int i2 = (j+1)*_y.length/_nOut;
			ret[2*j+0] = _xlo + i1*_xstep;
			ret[2*j+1] = 0;
			for (int i = i1; i < i2; i++) {
				ret[2*j+1] += _y[i];
			}
			ret[2*j+1] /= i2-i1;
		}
		return ret;
	}
	*/
	
	public double[] copyPartial(int nOut, double xmin, double xmax, double ymin, double ymax) {
		if (xmin >= xmax)
			return new double[] {};
		
		int ilo = max((int)floor((xmin - _xlo) / _xstep), 0);
		int ihi = min((int)ceil((xmax - _xlo) / _xstep), _y.length);
		nOut = min(nOut, ihi-ilo);
		double[] ret = new double[2*nOut];
		
		for (int j = 0; j < nOut; j++) {
			int i1 = j*    (ihi-ilo)/nOut;
			int i2 = (j+1)*(ihi-ilo)/nOut;
			ret[2*j+0] = _xlo + (i1+ilo)*_xstep;
			ret[2*j+1] = 0;
			for (int i = i1; i < i2; i++) {
				ret[2*j+1] += _y[i+ilo];
			}
			ret[2*j+1] /= i2-i1;
		}
		return ret;
	}
}
