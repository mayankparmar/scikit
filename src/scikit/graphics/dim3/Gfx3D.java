package scikit.graphics.dim3;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import scikit.util.Bounds;
import scikit.util.Point;

public class Gfx3D {
	private final GL gl;
	private final GLU glu = new GLU();
	private final GLUquadric gluq = glu.gluNewQuadric();
	
	public Gfx3D(GL gl) {
		this.gl = gl;
	}
	
	public GL getGL() {
		return gl;
	}
	
	public void projectOrtho3D(Bounds bds) {
	}
	
	public void setColor(Color color) {
		gl.glColor4fv(color.getComponents(null), 0);
	}
	
	public void drawCuboid(Bounds bds) {
		gl.glDisable(GL.GL_LIGHTING);
		gl.glBegin(GL.GL_LINES);
		double[] xs = {bds.xmin, bds.xmax};
		double[] ys = {bds.ymin, bds.ymax};
		double[] zs = {bds.zmin, bds.zmax};
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++) {
					if ((i + j + k) % 2 == 0) {
						gl.glVertex3d(xs[i],   ys[j],   zs[k]);
						gl.glVertex3d(xs[1-i], ys[j],   zs[k]);
						gl.glVertex3d(xs[i],   ys[j],   zs[k]);
						gl.glVertex3d(xs[i],   ys[1-j], zs[k]);
						gl.glVertex3d(xs[i],   ys[j],   zs[k]);
						gl.glVertex3d(xs[i],   ys[j],   zs[1-k]);
					}
				}
			}
		}
		gl.glEnd();
		gl.glEnable(GL.GL_LIGHTING);
	}
	
	public void drawSphere(Point center, double radius) {
		gl.glPushMatrix();
		gl.glTranslated(center.x, center.y, center.z);
		glu.gluSphere(gluq, radius, 8, 8);
		gl.glPopMatrix();
	}
}
