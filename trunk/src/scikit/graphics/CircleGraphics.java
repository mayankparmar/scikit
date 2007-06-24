package scikit.graphics;

import java.awt.Color;

import javax.media.opengl.GL;

import scikit.util.Bounds;
import static java.lang.Math.*;

public class CircleGraphics implements Graphics {
	private double _x, _y, _radius;	
	private Color _color = Color.BLACK;
	private Bounds _bounds;
	
	public CircleGraphics(double x, double y, double radius) {
		_x = x;
		_y = y;
		_radius = radius;
		_bounds = new Bounds(x-radius, x+radius, y-radius, y+radius);
	}
	
	public void draw(GL gl, Bounds bounds) {
		gl.glColor3fv(_color.getColorComponents(null), 0);
		gl.glBegin(GL.GL_LINE_LOOP);
		for (int i = 0; i < 64; i++) {
			double angle = 2.*PI*i/64.;
			gl.glVertex2d(_x + _radius*cos(angle), _y + _radius*sin(angle));
		}
		gl.glEnd();
	}

	public Bounds getBounds() {
		return _bounds;
	}
}
