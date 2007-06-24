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
	protected GLCanvas _canvas;
	protected Vector<Graphics> _graphics = new Vector<Graphics>();
	protected Bounds _curBounds = new Bounds();
	
	
	public Canvas() {
		GLCapabilities capabilities = new GLCapabilities();
		
		// For some unknown reason, enabling GL "sample buffers" actually make anti-aliased lines
		// look much worse on OS X. I guess it disables custom anti-aliasing code.
		//
		// capabilities.setSampleBuffers(true);
		// capabilities.setNumSamples(4);
		
		_canvas = new GLCanvas(capabilities);
		_canvas.addGLEventListener(new EventListener());
		_canvas.setPreferredSize(new Dimension(300, 300));
	}
	
	public Canvas(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
	}
	
	
	public GLCanvas getCanvas() {
		return _canvas;
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
				_canvas.display();
			}
		});		
	}
	
	public void clear() {
		removeAllGraphics();
		animate();
	}
	
	public void removeAllGraphics() {
		_graphics.clear();
	}
	
	public void addGraphics(Graphics g) {
		_graphics.add(g);
	}
	
	
	protected Bounds getCurrentBounds() {
		Bounds bounds = new Bounds();
		for (Graphics g : _graphics)
			bounds = (Bounds)bounds.createUnion(g.getBounds());
		
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
	
	
	protected void drawAllGraphics(GL gl, Bounds bounds) {
		for (Graphics g : _graphics) {
			g.draw(gl, bounds);
		}	
	}
	
	private class EventListener implements GLEventListener {
		public void display(GLAutoDrawable glDrawable) {
			GL gl = glDrawable.getGL();
			gl.glMatrixMode( GL.GL_PROJECTION );
			gl.glLoadIdentity(); // TODO necessary?
			setProjection(gl, _curBounds);
			
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			drawAllGraphics(gl, _curBounds);
		}

		public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
			// do nothing
		}

		public void init(GLAutoDrawable glDrawable) {
			GL gl = glDrawable.getGL();
			gl.glClearColor(1f, 1f, 1f, 0.0f);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL.GL_LINE_SMOOTH);
			// gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
			// gl.glEnable(GL.GL_DEPTH_TEST);
		}
		
		public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
			GL gl = glDrawable.getGL();
			gl.glViewport(0, 0, width, height);
		}
	}
}
