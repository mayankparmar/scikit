package kip.md;

import static java.lang.Math.*;
import static kip.util.MathPlus.*;

import java.util.ArrayList;

import scikit.util.Point;

public class PointGrid2D<Pt extends Point> {
	private double _L;
	private int _cols;
	private double _dx;
	private ArrayList<Pt> _cells[];
	
	@SuppressWarnings(value={"unchecked"})
	public PointGrid2D(double L, int cols) {
		_L = L;
		_cols = cols;
		_dx = L / cols;
		_cells = (ArrayList<Pt>[])new ArrayList[cols*cols];
		for (int i = 0; i < cols*cols; i++)
			_cells[i] = new ArrayList<Pt>();
	}
	
	
	public void clear() {
		for (ArrayList<Pt> e : _cells)
			e.clear();
	}
	
	
	public void addPoint(Pt pt) {
		_cells[pointToIndex(pt)].add(pt);
	}
	
	public ArrayList<Pt> pointsWithinRange(Point pt, double R) {
		int index = pointToIndex(pt);
		int i1 = index%_cols;
		int j1 = index/_cols;
		
		ArrayList<Pt> ret = new ArrayList<Pt>();
		int imax = (int)ceil(R/_dx);
		int d2Cutoff = (int) (sqr(R/_dx+sqrt(2))+1e-8);
		
		for (int di1 = -imax; di1 <= imax; di1++) {
			for (int dj1 = -imax; dj1 <= imax; dj1++) {
				if (di1*di1 + dj1*dj1 <= d2Cutoff) {
					int i2 = (i1+di1+_cols)%_cols;
					int j2 = (j1+dj1+_cols)%_cols;
					ret.addAll(_cells[_cols*j2+i2]);
				}
			}
		}
		return ret;
	}
	
	// rounding errors here are OK, as long as they occur in just this
	// one function
	private int pointToIndex(Point pt) {
		checkValidity(pt);
		int i = (int)(pt.x/_dx);
		int j = (int)(pt.y/_dx);
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
	
	private void checkValidity(Point pt) {
		if (pt.x < 0 || pt.x >= _L || pt.y < 0 || pt.y > _L)
			throw new IllegalArgumentException("Point " + pt + " exceeds range [0, "+_L+")");
	}
}
