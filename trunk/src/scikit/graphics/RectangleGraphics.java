package scikit.graphics;

import java.awt.Color;

import javax.media.opengl.GL;

import scikit.util.Bounds;

public class RectangleGraphics implements Graphics {
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
	
	public void draw(GL gl, Bounds bounds) {
		gl.glColor3fv(_color.getColorComponents(null), 0);
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glVertex2d(_x, _y);
		gl.glVertex2d(_x+_w, _y);
		gl.glVertex2d(_x+_w, _y+_h);
		gl.glVertex2d(_x, _y+_h);
		gl.glEnd();
	}

	public Bounds getBounds() {
		return _bounds;
	}
}
