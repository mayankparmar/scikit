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
	protected Vector<Drawable> drawables = new Vector<Drawable>();
	
	
	public Canvas() {
		GLCapabilities capabilities = new GLCapabilities();
		capabilities.setSampleBuffers(true);
		capabilities.setNumSamples(4);
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
		// the obvious doesn't work. for some reason, repaint() is not triggering display():
		//		canvas.repaint();
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
		drawables.clear();
		animate();
	}
	
	public void addDrawable(Drawable d) {
		drawables.add(d);
	}
	
	
	protected Bounds getBounds() {
		Bounds bounds = new Bounds();
		for (Drawable drawable : drawables)
			bounds = (Bounds)bounds.createUnion(drawable.getBounds());
		return bounds;
	}
	
	protected void display(GL gl) {
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadIdentity(); // TODO necessary?
		Bounds bounds = getBounds();
		(new GLU()).gluOrtho2D(bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		
		for (Drawable drawable : drawables) {
			drawable.draw(gl, getBounds());
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
