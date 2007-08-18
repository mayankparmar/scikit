package scikit.graphics;

import java.awt.Color;

public interface ColorChooser {
	/**
	 * Returns the color associated with value v.
	 * @param v
	 * @return The associated color.
	 */
	public Color getColor(double v);
}
