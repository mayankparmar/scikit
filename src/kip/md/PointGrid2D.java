package kip.md;

import static java.lang.Math.*;
import static kip.util.MathPlus.*;
import scikit.dataset.DynamicArray;
import scikit.util.Point;

public class PointGrid2D {
	private double _L;
	private int _cols;
	private double _dx;
	private DynamicArray _cells[];
	private boolean _periodic = true;
	
	@SuppressWarnings(value={"unchecked"})
	public PointGrid2D(double L, int cols) {
		_L = L;
		_cols = max(cols, 1);
		_dx = L / _cols;
		_cells = new DynamicArray[_cols*_cols];
		for (int i = 0; i < _cols*_cols; i++)
			_cells[i] = new DynamicArray();
	}
	
	
	public void usePeriodicBoundaryConditions(boolean periodic) {
		_periodic = periodic;
	}
	
	public void clear() {
		for (DynamicArray e : _cells)
			e.clear();
	}
	
	
	public void addPoint(double x, double y) {
		x = (x+_L)%_L;
		y = (y+_L)%_L;
		_cells[pointToIndex(x, y)].append2(x, y);
	}
	
	public void setPoints(double[] state, int stride, int N0, int N1) {
		clear();
		for (int i = N0; i < N1; i++) {
			addPoint(state[(2*i+0)*stride], state[(2*i+1)*stride]);
		}
	}
	
	public DynamicArray pointOffsetsWithinRange(double x, double y, double R) {
		x = (x+_L)%_L;
		y = (y+_L)%_L;
		int index = pointToIndex(x, y);
		int i1 = index%_cols;
		int j1 = index/_cols;
		
		DynamicArray ret = new DynamicArray();
		int imax = (int)(R/_dx+1.0);
		
		int d2Cutoff = (int) (sqr(R/_dx+sqrt(2))+1e-8);
		
		for (int di = -imax; di <= imax; di++) {
			for (int dj = -imax; dj <= imax; dj++) {
				if (di*di + dj*dj <= d2Cutoff) {
					int i2 = i1+di;
					int j2 = j1+dj;
					if (_periodic) {
						i2 = (i2+_cols)%_cols;
						j2 = (j2+_cols)%_cols;						
					}
					else if (min(i2,j2) < 0 || max(i2,j2) >= _cols) {
						continue;
					}
					
					DynamicArray cell = _cells[_cols*j2+i2];
					for (int n = 0; n < cell.size()/2; n++) {
						double dx = cell.get(2*n+0) - x + (i1+di-i2)*_dx;
						double dy = cell.get(2*n+1) - y + (j1+dj-j2)*_dx;
						ret.append2(dx, dy);
					}
				}
			}
		}
		return ret;
	}
	
	// rounding errors here are OK, as long as they occur in just this
	// one function
	private int pointToIndex(double x, double y) {
		int i = (int)(x/_dx);
		int j = (int)(y/_dx);
		assert(i < _cols && j < _cols);
		return j*_cols+i;
	}
	
	// returns center of grid element
	@SuppressWarnings("unused")
	private Point indexToPoint(int index) {
		int i = index%_cols;
		int j = index/_cols;
		return new Point((i+0.5)*_dx, (j+0.5)*_dx);
	}
}
