package scikit.graphics.dim3;

import java.awt.Component;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;

import scikit.graphics.Drawable;
import scikit.graphics.GLHelper;
import scikit.graphics.Scene;

public class Scene3D extends Scene<Gfx3D> {
	public Scene3D(String title) {
		super(title);
	}
	
	protected Component createComponent() {
		return GLHelper.createComponent(new GLHelper.DisplayListener() {
			public void display(GLAutoDrawable gd) {
				drawAll(new Gfx3D(gd.getGL()));
			}
		});
	}
	
	protected void drawAll(Gfx3D gd) {
		setProjection(gd);
		for (Drawable<Gfx3D> d : getAllDrawables())
			d.draw(gd);
	}
	
	private void setProjection(Gfx3D gd) {
		double fovY = 35;
		double aspect = _component.getWidth() / _component.getHeight();
		double zNear = 0.1;
		double zFar = 10;

		GL gl = gd.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		(new GLU()).gluPerspective(fovY, aspect, zNear, zFar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0f, 0f, -6f);
	}
}
