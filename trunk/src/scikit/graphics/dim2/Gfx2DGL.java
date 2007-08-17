package scikit.graphics.dim2;

import static java.lang.Math.*;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.GLUT;

import scikit.graphics.GLHelper;
import scikit.util.Bounds;


public class Gfx2DGL implements Gfx2D {
	private final GL gl;
	private final GLUT glut;
	private final Scene2D scene;
	private final Bounds pixBds;
	private Bounds datBds; 		// TODO rename to projBds
	
	private static int FONT = GLUT.BITMAP_8_BY_13;
	private static int FONT_HEIGHT = 8; // pixels
	
	
	// TODO replace scene by pixBds
	public Gfx2DGL(GL gl, Scene2D scene) {
		this.gl = gl;
		this.glut = new GLUT();
		this.scene = scene;
		datBds = pixBds = scene.pixelBounds();
	}
	
	public Scene2D scene() {
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
	
	public void setLineSmoothing(boolean b) {
		if (b)
			gl.glEnable(GL.GL_LINE_SMOOTH);
		else
			gl.glDisable(GL.GL_LINE_SMOOTH);
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
	
	public static GLCanvas createComponent(final Scene2D scene) {
		return GLHelper.createComponent(new GLHelper.DisplayListener() {
			public void display(GLAutoDrawable drawable) {
				scene.drawAll(new Gfx2DGL(drawable.getGL(), scene));
			}
		});
	}
}
