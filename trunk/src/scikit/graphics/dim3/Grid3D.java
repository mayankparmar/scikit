package scikit.graphics.dim3;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import scikit.graphics.ColorChooser;
import scikit.graphics.Drawable;
import scikit.util.Bounds;
import scikit.util.Point;

public class Grid3D extends Scene3D {
	double[] _data;
	int _w, _h, _d; // width, height, depth
	ColorChooser _colors;
    private boolean _autoScale = true;
    private double _lo = 0, _hi = 1;
    
    
	public Grid3D(String title) {
		super(title);
	}
	
	public void clear() {
		// remove data first because super.clear() will repaint() the component
		_w = _h = _d = 0;
		_data = null;
		super.clear();
	}
	
	public void setColors(ColorChooser colors) {
		_colors = colors;
	}
	
	public void setAutoScale() {
		_autoScale = true;
	}
	
	public void setScale(double lo, double hi) {
		_lo = lo;
		_hi = hi;
	}
	
	public void registerData(int w, int h, int d, double[] data) {
		setSize(w, h, d, data.length);
		System.arraycopy(data, 0, _data, 0, w*h*d);
		findRange();
		animate();
    }
	
	protected List<Drawable<Gfx3D>> getAllDrawables() {
		List<Drawable<Gfx3D>> ds = new ArrayList<Drawable<Gfx3D>>();
		ds.add(_gridDrawable);
		ds.addAll(super.getAllDrawables());
		return ds;
	}
	
	private void findRange() {
		if (_autoScale) {
			_lo = Double.POSITIVE_INFINITY;
			_hi = Double.NEGATIVE_INFINITY;
			for (double v : _data) {
				_lo = min(_lo, v);
				_hi = max(_hi, v);
			}
		}
	}
	
	private void setSize(int w, int h, int d, int expectedSize) {
		if (w*h*d == 0)
			throw new IllegalArgumentException(
					"Illegal specified shape (" + w + "*" + h + "*" + d + ")");
		if (w*h*d > expectedSize)
			throw new IllegalArgumentException("Array length " + expectedSize
					+ " does not fit specified shape (" + w + "*" + h + "*" + d + ")");
		if (w != _w || h != _h || d != _d) {
    		_w = w;
    		_h = h;
    		_d = d;
    		_data = new double[w*h*d];
    	}
	}

	private Drawable<Gfx3D> _gridDrawable = new Drawable<Gfx3D>() {
		public void draw(Gfx3D g) {
	        if (_data != null) {
 	        	for (int z = 0; z < _d; z++) { 
 	        		for (int y = 0; y < _h; y++) {
 	        			for (int x = 0; x < _w; x++) {
 	        				Color color = _colors.getColor(_data[_w*_h*z+_w*y+x], _lo, _hi);
 	        				if (color != null) {
 	        					g.setColor(color);
 	        					double cx = (x+0.5)/_w;
 	        					double cy = (y+0.5)/_h;
 	        					double cz = (z+0.5)/_d;
 	        					double radius = 1./_w;
 	        					g.drawSphere(new Point(cx, cy, cz), radius);
 	        				}
 	        			}
 	        		}
 	        	}
	        }
		}
		public Bounds getBounds() {
			return new Bounds(0, 1, 0, 1, 0, 1);
		}
	};
}
