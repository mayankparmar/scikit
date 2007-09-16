package scikit.graphics.dim3;

import scikit.graphics.ColorChooser;

public class Grid3D extends Scene3D {
	double[] _data;
	int _w, _h, _d; // width, height, depth
	ColorChooser _cc;
	
	public Grid3D(String title) {
		super(title);
	}
	
	public void registerData(int w, int h, int d, double[] data, ColorChooser cc) {
		setSize(w, h, d, data.length);
		System.arraycopy(data, 0, _data, 0, w*h*d);
		animate();
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

}
