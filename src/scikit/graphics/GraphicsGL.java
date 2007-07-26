package scikit.graphics;

import static java.lang.Math.*;

import java.awt.Color;
import java.awt.Dimension;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.SwingUtilities;

import com.sun.opengl.util.GLUT;

import scikit.util.Bounds;


public class GraphicsGL implements Graphics {
	private final GL gl;
	private final GLUT glut;
	private final Scene scene;
	private final Bounds pixBds;
	private Bounds datBds;
	
	private static int FONT = GLUT.BITMAP_8_BY_13;
	private static int FONT_HEIGHT = 8; // pixels
	
	
	public GraphicsGL(GL gl, Scene scene) {
		this.gl = gl;
		this.glut = new GLUT();
		this.scene = scene;
		datBds = pixBds = scene.pixelBounds();
	}
	
	public Object engine() {
		return gl;
	}

	public Scene scene() {
		return scene;
	}
	
	public void projectOrtho2D(Bounds proj) {
		// the GL projection matrix is 2d, in units of pixels
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		(new GLU()).gluOrtho2D(0, pixBds.xmax, 0, pixBds.ymax);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		// incoming points are transformed from datBds (data coordinates) 
		// into pixBds (pixel coordinates)
		this.datBds = proj;
	}
	
	private double transX(double x) {
		return pixBds.xmax * (x - datBds.xmin) / datBds.getWidth();
	}
	
	private double transY(double y) {
		return pixBds.ymax * (y - datBds.ymin) / datBds.getHeight();
	}
	
	private void vertex2d(GL gl, double x, double y) {
		gl.glVertex2d(transX(x), transY(y));
	}
	
	public void setColor(Color color) {
		gl.glColor4fv(color.getComponents(null), 0);
	}
	
	public void drawPoint(double x, double y) {
		gl.glBegin(GL.GL_POINTS);
		vertex2d(gl, x, y);
		gl.glEnd();
	}
	
	public void drawLine(double x1, double y1, double x2, double y2) {
		gl.glBegin(GL.GL_LINES);
		vertex2d(gl, x1, y1);
		vertex2d(gl, x2, y2);
		gl.glEnd();
	}
	
	public void drawLines(double[] xys) {
		gl.glBegin(GL.GL_LINES);
		for (int i = 0; i < xys.length; i++) {
			vertex2d(gl, xys[2*i+0], xys[2*i+1]);
		}
		gl.glEnd();
	}
	
	public void drawRect(double x, double y, double w, double h) {
		gl.glBegin(GL.GL_LINE_LOOP);
		vertex2d(gl, x, y);
		vertex2d(gl, x, y+h);
		vertex2d(gl, x+w, y+h);
		vertex2d(gl, x+w, y);
		gl.glEnd();
	}
	
	public void fillRect(double x, double y, double w, double h) {
		gl.glBegin(GL.GL_QUADS);
		vertex2d(gl, x, y);
		vertex2d(gl, x, y+h);
		vertex2d(gl, x+w, y+h);
		vertex2d(gl, x+w, y);
		gl.glEnd();
	}
	
	final private static int EDGES = 32;
	private static double[] COS = new double[EDGES], SIN = new double[EDGES];
	static {
		for (int i = 0; i < EDGES; i++) {
			COS[i] = cos((i/(double)EDGES)*2*PI);
			SIN[i] = sin((i/(double)EDGES)*2*PI);
		}
	}
	
	public void drawCircle(double x, double y, double r) {
		gl.glBegin(GL.GL_LINE_LOOP);
		for (int i = 0; i < EDGES; i++) {
			vertex2d(gl, x+r*COS[i], y+r*SIN[i]);
		}
		gl.glEnd();
		
	}
	
	public void fillCircle(double x, double y, double r) {
		gl.glBegin(GL.GL_POLYGON);
		for (int i = 0; i < EDGES; i++) {
			vertex2d(gl, x+r*COS[i], y+r*SIN[i]);
		}
		gl.glEnd();
	}

	public double stringWidth(String str) {
		return glut.glutBitmapLength(FONT, str) * datBds.getWidth() / pixBds.getWidth();
	}
	
	public double stringHeight(String str) {
		return FONT_HEIGHT * datBds.getHeight() / pixBds.getHeight();
	}
	
	public void drawString(String str, double x, double y) {
		gl.glPushMatrix();
		gl.glRasterPos2d(transX(x), transY(y));
		glut.glutBitmapString(FONT, str); 
		gl.glPopMatrix();
	}
	
	public static GLCanvas createComponent(final Scene scene) {
		GLCapabilities capabilities = new GLCapabilities();
		
		// For some unknown reason, enabling GL "sample buffers" actually make anti-aliased lines
		// look much worse on OS X. I guess it disables custom anti-aliasing code.
		//
		// capabilities.setSampleBuffers(true);
		// capabilities.setNumSamples(4);
		
		final GLCanvas canvas = new GLCanvas(capabilities) {
			private static final long serialVersionUID = 1L;
			// mystery: for some reason, repaint() doesn't work, so we must override to
			// explicitly call display() from GUI thread.
			public void repaint() {
				// N.B.: can't call display() from this thread, since its a blocking call, but
				// the GUI call is already blocked waiting for us!
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						display();
					}
				});
			}
		};
		
		canvas.addGLEventListener(new GLEventListener() {
			public void display(GLAutoDrawable glDrawable) {
				GL gl = glDrawable.getGL();
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				
				Graphics g = new GraphicsGL(gl, scene);
				g.projectOrtho2D(scene.dataBounds());
				for (Drawable d : scene.allDrawables())
					d.draw(g);
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
				gl.glLineWidth(1.0f);
				gl.glPointSize(4.0f);
			}
			
			public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
				GL gl = glDrawable.getGL();
				gl.glViewport(0, 0, width, height);
			}
		});
		
		canvas.setPreferredSize(new Dimension(300, 300));
		return canvas;
	}
}
