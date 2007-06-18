package scikit.graphics;

import java.awt.Canvas;
import javax.media.opengl.GL;

import scikit.util.Bounds;


public class TickMarks implements Drawable {
	private Canvas _canvas;
	
	public TickMarks(Canvas canvas) {
		_canvas = canvas;
	}
	
	public void draw(GL gl, Bounds bounds) {
	}

	public Bounds getBounds() {
		return new Bounds();
	}
}
