package scikit.graphics.dim3;

import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import scikit.graphics.Drawable;
import scikit.graphics.GLHelper;
import scikit.graphics.Scene;
import scikit.util.Bounds;
import scikit.util.Point;


public class Scene3D extends Scene<Gfx3D> {
	boolean _drawBounds = true;
	Quaternion _rotation = new Quaternion();
	
	public Scene3D(String title) {
		super(title);
		_component.addMouseListener(_mouseListener);
		_component.addMouseMotionListener(_mouseListener);
	}
	
	public void clear() {
		super.clear();
		_rotation = new Quaternion();
	}
	
	protected Component createComponent() {
		return GLHelper.createComponent(new GLEventListener() {
			public void display(GLAutoDrawable glDrawable) {
				drawAll(new Gfx3D(glDrawable.getGL()));
			}
			public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
			}
			public void init(GLAutoDrawable glDrawable) {
				GL gl = glDrawable.getGL();
				gl.glEnable(GL.GL_DEPTH_TEST);
				gl.glEnable(GL.GL_NORMALIZE);
				gl.glEnable(GL.GL_BLEND);
				gl.glEnable(GL.GL_COLOR_MATERIAL);
				gl.glEnable(GL.GL_LIGHTING);
				// gl.glEnable(GL.GL_LIGHT0);
				gl.glEnable(GL.GL_LIGHT1);
				gl.glShadeModel(GL.GL_SMOOTH);
				gl.glClearColor(1f, 1f, 1f, 0.0f);
				gl.glLineWidth(1.0f);
				gl.glPointSize(4.0f);
			    gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
			    gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, new float[]{0.9f,0.9f,0.9f,0.9f}, 0);
			    gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, new float[]{1,0.5f,1,0}, 0);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			}
			public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
				glDrawable.getGL().glViewport(0, 0, width, height);
			}
		});
	}
	
	protected void drawAll(Gfx3D gd) {
		GL gl = gd.getGL();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		if (viewBounds().getVolume() > 0) {
			setProjection(gd);
			for (Drawable<Gfx3D> d : getAllDrawables())
				d.draw(gd);
		}
	}
	
	protected List<Drawable<Gfx3D>> getAllDrawables() {
		List<Drawable<Gfx3D>> ds = new ArrayList<Drawable<Gfx3D>>();
		ds.addAll(super.getAllDrawables());
		if (_drawBounds)
			ds.add(Geom3D.cuboid(viewBounds(), Color.RED));
		return ds;
	}
	
	// TODO move to Gfx3D.projectPerspective3D
	private void setProjection(Gfx3D gd) {
		// get the corner to corner distance of the view bounds cuboid
		Bounds cb = viewBounds();
		double len = sqrt(sqr(cb.getWidth())+sqr(cb.getHeight())+sqr(cb.getDepth()));
		
		// set the projection matrix
		GL gl = gd.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		double fovY = 35;
		double aspect = (double)_component.getWidth() / _component.getHeight();
		double zNear = 0.1*len;
		double zFar = 10*len;
		(new GLU()).gluPerspective(fovY, aspect, zNear, zFar);
		
		// set the modelview matrix
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		// each sequential operation multiplies the modelview transformation matrix
		// from the left. operations on the scene object occur in reverse order from
		// their specification.
		// step (3): move object away from camera
		gl.glTranslated(0, 0, -1.8*len);
		// step (2): rotate object about zero
		gl.glMultMatrixd(_rotation.getRotationMatrix(), 0);
		// step (1): move object to its center
		Point center = cb.getCenter();
		gl.glTranslated(-center.x, -center.y, -center.z);
	}
	
	private MouseInputListener _mouseListener = new MouseInputAdapter() {
		java.awt.Point _lastDrag;
		
		public void mousePressed(MouseEvent event) {
			_lastDrag = event.getPoint();
		}
		
		public void mouseReleased(MouseEvent event) {
			_lastDrag = null;
		}
		
		public void mouseDragged(MouseEvent event) {
			if (_lastDrag == null)
				return; // this case did occur once, but i'm not sure how
			
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
