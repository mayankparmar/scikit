package scikit.graphics;

import scikit.util.Bounds;

public interface Drawable {
	public void draw(Graphics g);
	public Bounds getBounds();
}
