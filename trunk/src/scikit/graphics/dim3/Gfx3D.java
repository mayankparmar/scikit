package scikit.graphics.dim3;

import java.awt.Color;

import javax.media.opengl.GL;

import scikit.util.Bounds;

public class Gfx3D {
	private final GL gl;
	
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

}
