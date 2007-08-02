package scikit.graphics.dim3;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import scikit.graphics.Drawable;
import scikit.util.Bounds;

public class Geom3D {
	public static Drawable<GLAutoDrawable> cube(final Bounds bds, final Color color) {
		return new Drawable<GLAutoDrawable>() {
			public void draw(GLAutoDrawable gd) {
				GL gl = gd.getGL();
				gl.glColor4fv(color.getComponents(null), 0);
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
			}
			public Bounds getBounds() {
				return bds;
			}
		};
	}
}
