package scikit.graphics;

import java.awt.Color;

import scikit.util.Bounds;

public class RectangleGraphics implements Drawable {
	private double _x, _y, _w, _h;
	private Bounds _bounds;
	private Color _color = Color.BLACK;
	
	public RectangleGraphics(double x, double y, double w, double h) {
		_x = x;
		_y = y;
		_w = w;
		_h = h;
		_bounds = new Bounds(x, x+w, y, y+h);
	}
	
	public void draw(Graphics g) {
		g.setColor(_color);
		g.drawRect(_x, _y, _w, _h);
	}

	public Bounds getBounds() {
		return _bounds;
	}
}
