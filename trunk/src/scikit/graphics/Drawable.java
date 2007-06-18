package scikit.graphics;

import javax.media.opengl.GL;

import scikit.util.Bounds;

public interface Drawable {
	public void draw(GL gl, Bounds bounds);
	public Bounds getBounds();
}
