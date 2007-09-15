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
	Quaternion _rotation = new Quaternion();
	
	public Scene3D(String title) {
		super(title);
		_component.addMouseListener(_mouseListener);
		_component.addMouseMotionListener(_mouseListener);
	}
	
	protected Component createComponent() {
		return GLHelper.createComponent(new GLHelper.DisplayListener() {
			public void init(GLAutoDrawable gd) {
				gd.getGL().glEnable(GL.GL_DEPTH_TEST);
			}
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
		GL gl = gd.getGL();
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		double fovY = 35;
		double aspect = (double)_component.getWidth() / _component.getHeight();
		double zNear = 0.1;
		double zFar = 10;
		(new GLU()).gluPerspective(fovY, aspect, zNear, zFar);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0f, 0f, -6f);
		gl.glMultMatrixd(_rotation.getRotationMatrix(), 0);
	}
	
	private MouseInputListener _mouseListener = new MouseInputAdapter() {
		Point _lastDrag;
		public void mousePressed(MouseEvent event) {
			_lastDrag = event.getPoint();
		}
		
		public void mouseReleased(MouseEvent event) {
			_lastDrag = null;
		}
		
		public void mouseDragged(MouseEvent event) {
			double dx = event.getX() - _lastDrag.x;
			double dy = event.getY() - _lastDrag.y;
			_lastDrag = event.getPoint();
			
			double radPerPixel = 0.01;
			Quaternion q = new Quaternion();
			q.setFromRotationVector(radPerPixel*dy, radPerPixel*dx, 0);
			q.mul(_rotation);
			q.normalize();
			_rotation = q;
			_component.repaint();
		}
	};
}
