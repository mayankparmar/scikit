package scikit.graphics.dim3;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import scikit.graphics.ColorChooser;
import scikit.graphics.ColorGradient;
import scikit.graphics.Drawable;
import scikit.util.Bounds;

public class Grid3D extends Scene3D {
	private ColorChooser _colors = new ColorGradient();
	private int _w, _h, _d; // width, height, depth
	private double[] _data;
    private boolean _autoScale = true;
    private double _lo = 0, _hi = 1;
    private double _cutoff = 0.5;
    
    
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
	
	protected Component createComponent(Component canvas) {
		final JSlider slider = new JSlider(0, 1000, 500);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				_cutoff = slider.getValue()/1000.;
				_canvas.repaint();
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(super.createComponent(canvas), BorderLayout.CENTER);
		panel.add(slider, BorderLayout.SOUTH);
		return panel;
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
		if (w*h*d != expectedSize)
			throw new IllegalArgumentException("Array length " + expectedSize
					+ " does not fit specified shape (" + w + "*" + h + "*" + d + ")");
		if (w != _w || h != _h || d != _d) {
    		_w = w;
    		_h = h;
    		_d = d;
    		_data = new double[w*h*d];
    	}
	}
	
	private double getSample(int x, int y, int z) {
		if (x < 0 || x >= _w || y < 0 || y >= _h || z < 0 || z >= _d)
			return Double.NEGATIVE_INFINITY;
		double v = _data[_w*_h*z+_w*y+x];
		return (v - _lo) / (_hi - _lo);
	}
	
	private Color getColor(int x, int y, int z) {
		return _colors.getColor(_data[_w*_h*z+_w*y+x], _lo, _hi);
	}
	
	private static final double[][] _normals = new double[][] {
		{-1, 0, 0}, {+1, 0, 0},
		{0, -1, 0}, {0, +1, 0},
		{0, 0, -1}, {0, 0, +1}
	};
	private static final double[][] _dx = new double[][]{
			{0, 0, 0, 0},
			{0, 0, 0, 0},
			{+1, -1, -1, +1},
			{+1, -1, -1, +1},
			{+1, +1, -1, -1},
			{-1, -1, +1, +1},
	};
	private static final double[][] _dy = new double[][]{
			{+1, +1, -1, -1},
			{-1, -1, +1, +1},
			{0, 0, 0, 0},
			{0, 0, 0, 0},
			{+1, -1, -1, +1},
			{+1, -1, -1, +1}
	};
	private static final double[][] _dz = new double[][]{
			{+1, -1, -1, +1},
			{+1, -1, -1, +1},
			{+1, +1, -1, -1},
			{-1, -1, +1, +1},
			{0, 0, 0, 0},
			{0, 0, 0, 0}
	};
	private void drawPanel(Gfx3D g, double x, double y, double z, int dir) {
		GL gl = g.getGL();
		gl.glBegin(GL.GL_QUADS);
		gl.glNormal3dv(_normals[dir], 0);
		for (int i = 0; i < 4; i++) {
			gl.glVertex3d(
					(x+0.5*_dx[dir][i]+0.5)/_w,
					(y+0.5*_dy[dir][i]+0.5)/_h,
					(z+0.5*_dz[dir][i]+0.5)/_d);
		}
		gl.glEnd();
	}
	
	private Drawable<Gfx3D> _gridDrawable = new Drawable<Gfx3D>() {
		public void draw(Gfx3D g) {
	        if (_data != null) {
 	        	for (int z = 0; z < _d; z++) { 
 	        		for (int y = 0; y < _h; y++) {
 	        			for (int x = 0; x < _w; x++) {
 	        				if (getSample(x, y, z) >= _cutoff) {
 	        					g.setColor(getColor(x, y, z));
 	        					if (getSample(x-1, y, z) < _cutoff)
 	        						drawPanel(g, x-0.5, y, z, 0);
 	        					if (getSample(x+1, y, z) < _cutoff)
 	        						drawPanel(g, x+0.5, y, z, 1);
 	        					if (getSample(x, y-1, z) < _cutoff)
 	        						drawPanel(g, x, y-0.5, z, 2);
 	        					if (getSample(x, y+1, z) < _cutoff)
 	        						drawPanel(g, x, y+0.5, z, 3);
 	        					if (getSample(x, y, z-1) < _cutoff)
 	        						drawPanel(g, x, y, z-0.5, 4);
 	        					if (getSample(x, y, z+1) < _cutoff)
 	        						drawPanel(g, x, y, z+0.5, 5);
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
