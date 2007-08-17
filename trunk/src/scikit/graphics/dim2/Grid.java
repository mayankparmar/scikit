package scikit.graphics.dim2;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Component;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import scikit.graphics.ColorChooser;
import scikit.graphics.ColorScale;
import scikit.graphics.Drawable;
import scikit.util.Bounds;

public class Grid extends Scene2D {
	private BufferedImage _image;
	private int _w, _h;
	private double[] _data;
    private int[] _pixelArray;
    
	public Grid() {
		super();
	}
	
	public Grid(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
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
	
	public void registerColorScaleData(int w, int h, double[] data) {
		double lo = Double.POSITIVE_INFINITY;
		double hi = Double.NEGATIVE_INFINITY;
		for (double v : data) {
			lo = min(lo, v);
			hi = max(hi, v);
		}
		registerColorScaleData(w, h, data, lo, hi);
	}
	
	public void registerColorScaleData(int w, int h, double[] data, double lo, double hi) {
		registerData(w, h, data, new ColorScale(lo, hi));
	}
	
	public void registerData(int w, int h, double[] data, ColorChooser cc) {
		setSize(w, h);
		System.arraycopy(data, 0, _data, 0, w*h);
		rasterizeImage(cc);
		animate();
    }
	
	private void setSize(int w, int h) {
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
	        	Gfx2DSwing g2 = (Gfx2DSwing)g;
	        	int x1 = g2.transX(0);
	        	int y1 = g2.transY(0);
	        	int x2 = g2.transX(1);
	        	int y2 = g2.transY(1);
				int w = _image.getWidth();
				int h = _image.getHeight();
				g2.engine().setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	            g2.engine().drawImage(_image, x1, y1, x2, y2, 0, 0, w, h, null);
	        }
		}
		public Bounds getBounds() {
			return new Bounds(0, 1, 0, 1);
		}
	};
}
