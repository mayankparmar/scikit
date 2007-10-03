package scikit.graphics.dim3;

import static scikit.util.Utilities.format;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import scikit.graphics.ColorChooser;
import scikit.graphics.ColorGradient;
import scikit.graphics.Drawable;
import scikit.util.Bounds;
import scikit.util.FileUtil;

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
	
	public void extractData(double[] dst) {
		System.arraycopy(_data, 0, dst, 0, _data.length);
	}
	
	public void saveData(String fname) {
		try {
			fname = FileUtil.saveDialog(_component, fname);
			if (fname != null) {
				DataOutputStream dos = FileUtil.dosFromString(fname);
				dos.writeInt(_w);
				dos.writeInt(_h);
				dos.writeInt(_d);
				for (double v : _data)
					dos.writeDouble(v);
				dos.close();
			}
		} catch (IOException e) {}
	}
	
	public void loadData(String fname) {
		try {
			fname = FileUtil.loadDialog(_component, fname);
			if (fname != null) {
				DataInputStream dis = FileUtil.disFromString(fname);
				int w = dis.readInt();
				int h = dis.readInt();
				int d = dis.readInt();
				double[] data = new double[w*h*d];
				for (int i = 0; i < w*h*d; i++)
					data[i] = dis.readDouble();
				dis.close();
				registerData(w, h, d, data);
			}
		} catch (IOException e) {}		
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
	
	protected List<JMenuItem> getAllPopupMenuItems() {
		List<JMenuItem> ret = new ArrayList<JMenuItem>(super.getAllPopupMenuItems());
		if (_data != null) {
			JMenuItem menuItem = new JMenuItem("Save grid data ...");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveData("grid.dat");
				}
			});
			ret.add(menuItem);
		}
		JMenuItem menuItem = new JMenuItem("Load grid data ...");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadData("");
			}
		});
		ret.add(menuItem);
		
		return ret;
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
	        
			g.ortho2D(g.pixelBounds());
			g.setColor(Color.BLACK);
        	g.rasterString("lo = "+format(_lo), 10, 10+1.5*g.stringHeight(null));
        	g.rasterString("hi = "+format(_hi), 10, 10);
        	g.perspective3D(g.viewBounds());
		}
		public Bounds getBounds() {
			return new Bounds(0, 1, 0, 1, 0, 1);
		}
	};
}
