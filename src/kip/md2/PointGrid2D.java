package kip.md2;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;

import java.util.ArrayList;

import scikit.util.Point;
import scikit.util.Utilities;

public class PointGrid2D<Pt extends Point> {
	private double _L;
	private int _cols;
	private double _dx;
	private Pt[] _points;
	private ArrayList<Pt>[] _cells;

	@SuppressWarnings("unchecked")
	public PointGrid2D(double L, int cols, Pt[] points) {
		_L = L;
		_cols = max(cols, 1);
		_dx = L / _cols;
		_points = points;
		_cells = new ArrayList[_cols*_cols];
		for (int i = 0; i < _cols*_cols; i++)
			_cells[i] = new ArrayList<Pt>();
		initialize();
	}

	public void initialize() {
		for (ArrayList<Pt> c : _cells)
			c.clear();
		for (Pt p : _points) {
			_cells[pointToIndex(p.x, p.y)].add(p);
		}
	}
	
	public ArrayList<Pt> pointOffsetsWithinRange(Point p, double R) {
		double x = (p.x+_L)%_L;
		double y = (p.y+_L)%_L;
		int index = pointToIndex(x, y);
		int i1 = index%_cols;
		int j1 = index/_cols;
		
		ArrayList<Pt> ret = new ArrayList<Pt>();
		int imax = (int)(R/_dx+1.0);
		int d2Cutoff = (int) (sqr(R/_dx+sqrt(2))+1e-8);
		
		for (int di = -imax; di <= imax; di++) {
			for (int dj = -imax; dj <= imax; dj++) {
				if (di*di + dj*dj <= d2Cutoff) {
					int i2 = (i1+di+_cols)%_cols;
					int j2 = (j1+dj+_cols)%_cols;
					ret.addAll(_cells[_cols*j2+i2]);
				}
			}
		}
		return ret;
	}
	
	// for testing purposes only
	public ArrayList<Pt> pointOffsetsWithinRangeSlow(Point p, double R) {
		ArrayList<Pt> ret = new ArrayList<Pt>();
		
		for (int i = 0; i < _points.length; i++) {
			double dx = abs(p.x - _points[i].x);
			double dy = abs(p.y - _points[i].y);
			dx = Utilities.periodicOffset(_L, dx);
			dy = Utilities.periodicOffset(_L, dy);
			if (dx*dx + dy*dy < R*R)
				ret.add(_points[i]);
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
