package scikit.graphics;

import java.awt.Dimension;
import java.util.Vector;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.SwingUtilities;

import scikit.jobs.Display;
import scikit.util.Bounds;




public class Canvas implements Display {
	protected GLCanvas canvas;
	protected Vector<Graphics> drawables = new Vector<Graphics>();
	protected Bounds _curBounds = new Bounds();
	
	
	public Canvas() {
		GLCapabilities capabilities = new GLCapabilities();
		capabilities.setSampleBuffers(true);
		capabilities.setNumSamples(2);
		canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(new EventListener());
		canvas.setPreferredSize(new Dimension(300, 300));
	}
	
	public Canvas(String title) {
		this();
		scikit.util.Utilities.frame(canvas, title);
	}
	
	
	public GLCanvas getCanvas() {
		return canvas;
	}
	
	public void animate() {
		_curBounds = getCurrentBounds();
		
		// mystery: the obvious doesn't work. for some reason, repaint() is not triggering display():
		// //		canvas.repaint();
		//
		// so we have to force a display in the GUI thread instead.
		// N.B.: can't call display() from this thread, since its a blocking call, but
		// the GUI call is already blocked waiting for us!
		//
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				canvas.display();
			}
		});		
	}
	
	public void clear() {
		removeAllGraphics();
		animate();
	}
	
	public void removeAllGraphics() {
		drawables.clear();
	}
	
	public void addGraphics(Graphics d) {
		drawables.add(d);
	}
	
	
	protected Bounds getCurrentBounds() {
		Bounds bounds = new Bounds();
		for (Graphics drawable : drawables)
			bounds = (Bounds)bounds.createUnion(drawable.getBounds());
		
		// extend bounds a little bit
		double w = bounds.xmax - bounds.xmin;
		double h = bounds.ymax - bounds.ymin;
		bounds.xmin -= w/16;
		bounds.xmax += w/16;
		bounds.ymin -= h/16;
		bounds.ymax += h/16;
		return bounds;
	}
	
	
	protected void setProjection(GL gl, Bounds bounds) {
		(new GLU()).gluOrtho2D(bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);
	}
	
	protected void display(GL gl) {
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadIdentity(); // TODO necessary?
		setProjection(gl, _curBounds);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		for (Graphics drawable : drawables) {
			drawable.draw(gl, _curBounds);
		}
	}
	
	
	private class EventListener implements GLEventListener {
		public void display(GLAutoDrawable glDrawable) {
			Canvas.this.display(glDrawable.getGL());
		}

		public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
			// do nothing
		}

		public void init(GLAutoDrawable glDrawable) {
			GL gl = glDrawable.getGL();
			gl.glClearColor(1f, 1f, 1f, 0.0f);
			gl.glEnable(GL.GL_LINE_SMOOTH);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
		}
		
		public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
			GL gl = glDrawable.getGL();
			gl.glViewport(0, 0, width, height);
		}
	}
}
