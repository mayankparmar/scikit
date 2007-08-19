package scikit.graphics.dim2;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import scikit.graphics.ColorChooser;
import scikit.graphics.ColorGradient;
import scikit.graphics.Drawable;
import scikit.util.Bounds;

public class Grid extends Scene2D {
	private BufferedImage _image = null;
	private int _w = 0, _h = 0;
	private double[] _data = null;
    private int[] _pixelArray;
    
	public Grid() {
		super();
	}
	
	public Grid(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	public void clear() {
		// remove data first because super.clear() will cause a drawAll() operation
		_w = _h = 0;
		_image = null;
		_data = null;
		super.clear();
	}
	
	protected Component createComponent() {
		return Gfx2DSwing.createComponent(this);
	}
	
	protected void drawBackground(Gfx2D g) {
		// do not draw background for possible performance benefit
	}
	
	protected List<Drawable<Gfx2D>> getAllDrawables() {
		List<Drawable<Gfx2D>> ds = new ArrayList<Drawable<Gfx2D>>();
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
					saveData("grid.txt");
				}
			});
			ret.add(menuItem);
		}
		return ret;
	}
	
	public void registerData(int w, int h, double[] data) {
		double lo = Double.POSITIVE_INFINITY;
		double hi = Double.NEGATIVE_INFINITY;
		for (double v : data) {
			lo = min(lo, v);
			hi = max(hi, v);
		}
		registerData(w, h, data, lo, hi);
	}
	
	public void registerData(int w, int h, double[] data, double lo, double hi) {
		registerData(w, h, data, new ColorGradient(lo, hi));
	}
	
	public void registerData(int w, int h, double[] data, ColorChooser cc) {
		setSize(w, h, data.length);
		System.arraycopy(data, 0, _data, 0, w*h);
		rasterizeImage(cc);
		animate();
    }
	
	public void registerData(int w, int h, int[] data, ColorChooser cc) {
		setSize(w, h, data.length);
		for (int i = 0; i < data.length; i++)
			_data[i] = data[i];
		rasterizeImage(cc);
		animate();
	}
	
	private void setSize(int w, int h, int expectedSize) {
		if (w*h == 0)
			throw new IllegalArgumentException("Illegal specified shape (" + w + "*" + h + ")");
		if (w*h > expectedSize)
			throw new IllegalArgumentException("Array length " + expectedSize
					+ " does not fit specified shape (" + w + "*" + h + ")");
		if (w != _w || h != _h) {
    		_w = w;
    		_h = h;
    		_data = new double[w*h];
    		_pixelArray = new int[w*h*3];
    		_image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    	}
	}
	
	private void rasterizeImage(ColorChooser cc) {
        int pixelArrayOffset = 0;
        for (int y = 0; y < _h; y++) {
            for (int x = 0; x < _w; x++) {
                Color color = cc.getColor(_data[_w*y+x]);
                _pixelArray[pixelArrayOffset++] = color.getRed();
                _pixelArray[pixelArrayOffset++] = color.getGreen();
                _pixelArray[pixelArrayOffset++] = color.getBlue();
            }
        }
        WritableRaster raster = _image.getRaster();
        raster.setPixels(0, 0, _w, _h, _pixelArray);
	}
	
	private Drawable<Gfx2D> _gridDrawable = new Drawable<Gfx2D>() {
		public void draw(Gfx2D g) {
	        if (_image != null) {
	        	((Gfx2DSwing)g).renderImage(_image, 0, 0, 1, 1);
	        }
		}
		public Bounds getBounds() {
			return new Bounds(0, 1, 0, 1);
		}
	};
	
	private void saveData(String str) {
		try {
			PrintWriter pw = scikit.util.Dump.pwFromDialog(_component, str);
			if (pw != null)
				scikit.util.Dump.writeOctaveGrid(pw, _data, _w, 1);
		} catch (IOException e) {}
	}
}
