package scikit.dataset;

import static java.lang.Math.*;

public class Coarsened extends DataSet {
	private double[] _y, _cgy;
	private double _xlo, _xstep;
	private int _ilo, _ihi, _istep;
	
	
	public Coarsened(double[] y, double xlo, double xhi, double cgstep) {
		this(y, xlo, xhi, xlo, xhi, cgstep);
	}
	
	public Coarsened(double[] y, double xlo, double xhi, double cglo, double cghi, double cgstep) {
		_y = y;
		_xlo = xlo;
		_xstep = (xhi-xlo)/(y.length-1);
		
		_ilo = (int) ((cglo - xlo) / _xstep);
		_ihi = (int) ((cghi - xlo) / _xstep) + 1;
		_ilo = max(_ilo, 0);
		_ihi = max(min(_ihi, y.length), _ilo);
		
		setBinWidth(cgstep);
	}
	
	public void setBinWidth(double cgstep) {
		_istep = (int) max(cgstep/_xstep, 1);
		int n = (_ihi - _ilo) / _istep;
		if (_cgy == null || _cgy.length != n) {
			_cgy = new double[n];
		}
	}
	
	public double[] copyData() {
		updateAll();
		
		double[] ret = new double[2*_cgy.length];
		for (int j = 0; j < _cgy.length; j++) {
			ret[2*j+0] = _xlo + _xstep*(_istep*(j+0.5) - 0.5);
			ret[2*j+1] = _cgy[j];
		}
		return ret;
	}

	private void updateAll() {
		for (int j = 0; j < _cgy.length; j++) {
			_cgy[j] = 0;
			for (int i = _ilo + j*_istep; i < _ilo + (j+1)*_istep; i++)
				_cgy[j] += _y[i];
			_cgy[j] /= _istep;
		}
	}
}
