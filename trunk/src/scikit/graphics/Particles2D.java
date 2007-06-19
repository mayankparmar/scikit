package scikit.graphics;

import java.awt.Color;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import scikit.util.Bounds;

public class Particles2D implements Drawable {
	private GLU glu = new GLU();
	private GLUquadric quadric = glu.gluNewQuadric();

	private Bounds _bounds;
	private Color _color;
	private double _radius;
	
	private double[] _phase;
	private int _stride;
	private int _N0;
	private int _N1;
	
	
	public Particles2D(double[] phase, double radius, double L, Color color) {
		_phase = phase;
		_radius = radius;
		_bounds = new Bounds(0, L, 0, L);
		_color = color;
		
		_stride = 1;
		_N0 = 0;
		_N1 = phase.length/(2*_stride);
	}
	
	public void draw(GL gl, Bounds bounds) {
		gl.glColor4fv(_color.getComponents(null), 0);
		for (int i = _N0; i < _N1; i++) {
			drawCircle(gl, _phase[(2*i+0)*_stride], _phase[(2*i+1)*_stride]);
		}
	}

	public Bounds getBounds() {
		return _bounds;
	}
	
	
	public void setPhaseArrayFormat(int stride, int N0, int N1) {
		_stride = stride;
		_N0 = N0;
		_N1 = N1;
	}
	
	private void drawCircle(GL gl, double x, double y) {
		gl.glPushMatrix();		
		gl.glTranslated(x, y, 0);
		glu.gluDisk(quadric, 0, _radius, 32, 1);
		gl.glPopMatrix();
	}
}
