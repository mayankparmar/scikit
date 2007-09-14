package scikit.graphics.dim3;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import scikit.graphics.Drawable;
import scikit.graphics.GLHelper;
import scikit.graphics.Scene;
import scikit.util.Bounds;

public class Scene3D extends Scene<Gfx3D> {
	public Scene3D(String title) {
		super(title);
		_component.addMouseListener(_mouseListener);
		_component.addMouseMotionListener(_mouseListener);
	}
	
	protected Component createComponent() {
		return GLHelper.createComponent(new GLHelper.DisplayListener() {
			public void display(GLAutoDrawable gd) {
				drawAll(new Gfx3D(gd.getGL()));
			}
		});
	}
	
	protected void drawAll(Gfx3D gd) {
		setProjection(gd);
		Geom3D.cube(new Bounds(-1, 1, -1, 1, -1, 1), Color.RED).draw(gd);
		for (Drawable<Gfx3D> d : getAllDrawables())
			d.draw(gd);
	}
	
	private void setProjection(Gfx3D gd) {
		double fovY = 35;
		double aspect = _component.getWidth() / _component.getHeight();
		double zNear = 0.1;
		double zFar = 10;

		GL gl = gd.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		(new GLU()).gluPerspective(fovY, aspect, zNear, zFar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0f, 0f, -6f);
	}
	
	Point _lastClick;
	
	private MouseInputListener _mouseListener = new MouseInputAdapter() {
		public void mousePressed(MouseEvent event) {
			_lastClick = event.getPoint();
		}
		
		public void mouseReleased(MouseEvent event) {
			_lastClick = null;
		}
		public void mouseDragged(MouseEvent event) {
//			double dx = _lastClick.x - event.getX();
//			double dy = _lastClick.y - event.getY();
			_component.repaint();
		}
	};
}
