package scikit.graphics;

import java.awt.Color;
import scikit.util.Bounds;

public class CircleGraphics implements Drawable {
	private double _x, _y, _radius;	
	private Color _color = Color.BLACK;
	private Bounds _bounds;
	
	public CircleGraphics(double x, double y, double radius) {
		_x = x;
		_y = y;
		_radius = radius;
		_bounds = new Bounds(x-radius, x+radius, y-radius, y+radius);
	}
	
	public void draw(Graphics g) {
		g.setColor(_color);
		g.drawCircle(_x, _y, _radius);
	}

	public Bounds getBounds() {
		return _bounds;
	}
}
