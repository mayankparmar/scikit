package scikit.graphics.dim3;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import scikit.graphics.Drawable;
import scikit.graphics.GLHelper;
import scikit.graphics.Scene;
import scikit.numerics.vecmath.Quat4d;
import scikit.numerics.vecmath.VecHelper;


public class Scene3D extends Scene<Gfx3D> {
	protected final static double RADS_PER_PIXEL = 0.01;
	protected boolean _drawBounds = true;
	protected Quat4d _rotation = new Quat4d(0, 0, 0, 1);
	
	
	public Scene3D(String title) {
		super(title);
		_canvas.addMouseListener(_mouseListener);
		_canvas.addMouseMotionListener(_mouseListener);
	}
	
	public void clear() {
		super.clear();
		_rotation = new Quat4d(0, 0, 0, 1);
	}
	
	public Quat4d getRotation() {
		return _rotation;
	}
	
	protected Component createCanvas() {
		return GLHelper.createComponent(new GLEventListener() {
			public void display(GLAutoDrawable glDrawable) {
				Gfx3D g = new Gfx3D(glDrawable, Scene3D.this);
				drawAll(g);
			}
			public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
			}
			public void init(GLAutoDrawable glDrawable) {
				GL gl = glDrawable.getGL();
				gl.glEnable(GL.GL_NORMALIZE);
				gl.glEnable(GL.GL_BLEND);
				gl.glEnable(GL.GL_CULL_FACE);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				gl.glShadeModel(GL.GL_SMOOTH);
				gl.glClearColor(1f, 1f, 1f, 0.0f);
				gl.glLineWidth(1.0f);
				gl.glPointSize(4.0f);
			}
			public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
				glDrawable.getGL().glViewport(0, 0, width, height);
			}
		});
	}
	
	protected void drawAll(Gfx3D g) {
		g.getGL().glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		if (g.viewBounds().getVolume() > 0) {
			g.perspective3D(g.viewBounds());
			for (Drawable<Gfx3D> d : getAllDrawables())
				d.draw(g);
		}
	}
	
	protected List<Drawable<Gfx3D>> getAllDrawables() {
		List<Drawable<Gfx3D>> ds = new ArrayList<Drawable<Gfx3D>>();
		ds.addAll(super.getAllDrawables());
		if (_drawBounds)
			ds.add(Geom3D.cuboid(viewBounds(), Color.GREEN));
		return ds;
	}
	
	protected void handleMouseDrag(double dx, double dy, MouseEvent event) {
		Quat4d q = VecHelper.quatFromAxisAngle(RADS_PER_PIXEL*dy, RADS_PER_PIXEL*dx, 0);
		_rotation.mul(q, _rotation);
		_rotation.normalize();
	}
	
	private MouseInputListener _mouseListener = new MouseInputAdapter() {
		java.awt.Point _lastDrag;
		
		public void mousePressed(MouseEvent event) {
			if (!event.isPopupTrigger())
				_lastDrag = event.getPoint();
		}
		
		public void mouseReleased(MouseEvent event) {
			_lastDrag = null;
		}
		
		public void mouseDragged(MouseEvent event) {
			if (_lastDrag == null)
				return;
			
			double dx = event.getX() - _lastDrag.x;
			double dy = event.getY() - _lastDrag.y;
			_lastDrag = event.getPoint();
			
			handleMouseDrag(dx, dy, event);
			_canvas.repaint();
		}
	};
}
